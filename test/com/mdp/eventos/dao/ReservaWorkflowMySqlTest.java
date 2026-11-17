package com.mdp.eventos.dao;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.impl.ReservaDAOImpl;
import com.mdp.eventos.dao.impl.UsuarioDAOImpl;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.service.SalonService;
import com.mdp.eventos.service.SeguridadSesion;
import com.mdp.eventos.service.ServicioAdicionalService;
import com.mdp.eventos.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "MDP_TEST_DB_URL", matches = "jdbc:mysql:.+")
class ReservaWorkflowMySqlTest {
    private final String unique = UUID.randomUUID().toString().replace("-", "");
    private long adminId;
    private long proveedorId;
    private long clienteId;
    private long zonaId;
    private long tipoEventoId;
    private long establecimientoId;
    private long salonId;
    private long otroProveedorId;
    private long otroEstablecimientoId;
    private long otroSalonId;
    private String adminEmail;
    private String proveedorEmail;

    @BeforeAll
    static void configurarBaseDePrueba() {
        System.setProperty("db.url", System.getenv("MDP_TEST_DB_URL"));
        System.setProperty("db.user", System.getenv().getOrDefault("MDP_TEST_DB_USER", ""));
        System.setProperty("db.password", System.getenv().getOrDefault("MDP_TEST_DB_PASSWORD", ""));
    }

    @BeforeEach
    void crearDatosAislados() throws Exception {
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            adminEmail = "test-admin-" + unique + "@example.test";
            proveedorEmail = "test-provider-" + unique + "@example.test";
            String clienteEmail = "test-client-" + unique + "@example.test";
            adminId = crearUsuario(connection, Rol.ROLE_ADMIN, adminEmail);
            proveedorId = crearUsuario(connection, Rol.ROLE_PROVEEDOR, proveedorEmail);
            clienteId = crearUsuario(connection, Rol.ROLE_CLIENTE, clienteEmail);

            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO zonas_mdp (nombre, codigo_postal, activo) VALUES (?, '7600', TRUE)",
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, "Zona test " + unique);
                statement.executeUpdate();
                zonaId = generatedId(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tipos_evento (nombre, descripcion, activo) VALUES (?, 'Fixture', TRUE)",
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, "Evento test " + unique);
                statement.executeUpdate();
                tipoEventoId = generatedId(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO establecimientos
                    (proveedor_id, zona_id, cuit, nombre_comercial, direccion, activo)
                VALUES (?, ?, ?, ?, 'Dirección de prueba', TRUE)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setLong(1, proveedorId);
                statement.setLong(2, zonaId);
                statement.setString(3, "20" + unique.substring(0, 9));
                statement.setString(4, "Establecimiento test " + unique);
                statement.executeUpdate();
                establecimientoId = generatedId(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO salones
                    (establecimiento_id, nombre_salon, capacidad_minima, capacidad_maxima,
                     hora_apertura, hora_cierre, tarifa_base_hora, costo_por_persona, activo)
                VALUES (?, ?, 1, 200, '00:00:00', '00:00:00', 100.00, 1.00, TRUE)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setLong(1, establecimientoId);
                statement.setString(2, "Salón test " + unique);
                statement.executeUpdate();
                salonId = generatedId(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO salon_tipo_evento (salon_id, tipo_evento_id) VALUES (?, ?)"
            )) {
                statement.setLong(1, salonId);
                statement.setLong(2, tipoEventoId);
                statement.executeUpdate();
            }
        }
    }

    @AfterEach
    void limpiarDatos() throws Exception {
        SesionActual.cerrar();
        SesionActual.configurarInvalidacion(() -> { });
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            if (salonId > 0) ejecutar(connection, "DELETE FROM reservas WHERE salon_id = ?", salonId);
            if (salonId > 0) ejecutar(connection, "DELETE FROM salones WHERE id = ?", salonId);
            if (otroSalonId > 0) ejecutar(connection, "DELETE FROM salones WHERE id = ?", otroSalonId);
            if (establecimientoId > 0) ejecutar(connection, "DELETE FROM establecimientos WHERE id = ?", establecimientoId);
            if (otroEstablecimientoId > 0) {
                ejecutar(connection, "DELETE FROM establecimientos WHERE id = ?", otroEstablecimientoId);
            }
            if (tipoEventoId > 0) ejecutar(connection, "DELETE FROM tipos_evento WHERE id = ?", tipoEventoId);
            if (zonaId > 0) ejecutar(connection, "DELETE FROM zonas_mdp WHERE id = ?", zonaId);
            if (adminId > 0 && proveedorId > 0 && clienteId > 0) {
                ejecutar(connection, "DELETE FROM usuarios WHERE id IN (?, ?, ?)", adminId, proveedorId, clienteId);
            }
            if (otroProveedorId > 0) ejecutar(connection, "DELETE FROM usuarios WHERE id = ?", otroProveedorId);
        }
    }

    @Test
    void dosReservasConcurrentesNoPuedenOcuparElMismoHorarioYGuardaVencimiento() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(3);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primera = executor.submit(() -> intentarReservar(inicio, fecha, "A"));
            Future<Boolean> segunda = executor.submit(() -> intentarReservar(inicio, fecha, "B"));
            inicio.countDown();
            assertTrue(primera.get() ^ segunda.get(), "Solo una de las reservas concurrentes debe confirmarse.");
        } finally {
            executor.shutdownNow();
        }
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT COUNT(*), MIN(TIMESTAMPDIFF(MINUTE, CURRENT_TIMESTAMP, vence_en)) FROM reservas WHERE salon_id = ?"
             )) {
            statement.setLong(1, salonId);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                assertEquals(1, row.getInt(1));
                assertTrue(row.getLong(2) >= 1439 && row.getLong(2) <= 1441,
                    "La solicitud debe vencer a las 24 horas.");
            }
        }
    }

    @Test
    void calculaVencimientoAlInicioYFinalizacionAlTerminarElEvento() throws Exception {
        long reservaVencida = insertarReserva(
            LocalDate.now().plusDays(5), LocalTime.of(18, 0), LocalTime.of(20, 0),
            "PENDIENTE", "vence-" + unique, true, false
        );
        long inicioPasado = insertarReserva(
            LocalDate.now().minusDays(1), LocalTime.of(23, 0), LocalTime.of(23, 30),
            "PENDIENTE", "inicio-" + unique, false, false
        );
        long finalizada = insertarReserva(
            LocalDate.now().minusDays(2), LocalTime.of(18, 0), LocalTime.of(20, 0),
            "CONFIRMADA", "finalizada-" + unique, false, true
        );

        new ReservaDAOImpl().actualizarEstadosAutomaticos();

        assertEquals("VENCIDA", estado(reservaVencida));
        assertEquals("VENCIDA", estado(inicioPasado));
        assertEquals("FINALIZADA", estado(finalizada));
    }

    @Test
    void bajaDeProveedorCancelaPendientesYMarcaConfirmadasSinCancelarlas() throws Exception {
        long pendiente = insertarReserva(
            LocalDate.now().plusDays(3), LocalTime.of(18, 0), LocalTime.of(20, 0),
            "PENDIENTE", "pendiente-" + unique, false, false
        );
        long confirmada = insertarReserva(
            LocalDate.now().plusDays(4), LocalTime.of(18, 0), LocalTime.of(20, 0),
            "CONFIRMADA", "confirmada-" + unique, false, false
        );
        Usuario admin = new UsuarioDAOImpl().buscarPorEmail(adminEmail).orElseThrow();
        Usuario proveedor = new UsuarioDAOImpl().buscarPorEmail(proveedorEmail).orElseThrow();
        SesionActual.iniciar(admin);

        new UsuarioService(new UsuarioDAOImpl()).actualizarEstado(proveedorId, false);

        assertEquals("CANCELADA", estado(pendiente));
        assertEquals("Proveedor dado de baja", motivoCancelacion(pendiente));
        assertEquals(adminId, canceladaPor(pendiente));
        assertTrue(canceladaEnRegistrada(pendiente));
        assertEquals("CONFIRMADA", estado(confirmada));
        assertTrue(requiereRevision(confirmada));

        AtomicBoolean redirigida = new AtomicBoolean();
        CountDownLatch invalidacion = new CountDownLatch(1);
        SesionActual.configurarInvalidacion(() -> {
            redirigida.set(true);
            invalidacion.countDown();
        });
        SesionActual.iniciar(proveedor);
        assertThrows(BusinessValidationException.class,
            () -> SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR));
        assertTrue(invalidacion.await(2, TimeUnit.SECONDS));
        assertTrue(redirigida.get());
        assertTrue(SesionActual.getUsuario().isEmpty());
        SesionActual.configurarInvalidacion(() -> { });
    }

    @Test
    void verificaRolYValidacionesDeServicioEnLaCapaDeServicio() throws Exception {
        Usuario cliente = new UsuarioDAOImpl().buscarPorEmail("test-client-" + unique + "@example.test").orElseThrow();
        SalonService service = new SalonService(new com.mdp.eventos.dao.impl.SalonDAOImpl());
        Salon salon = new Salon();
        salon.setId(salonId);
        salon.setEstablecimientoId(establecimientoId);
        salon.setNombreSalon("Salón test " + unique);
        salon.setCapacidadMinima(20);
        salon.setCapacidadMaxima(10);
        salon.setHoraApertura(LocalTime.MIDNIGHT);
        salon.setHoraCierre(LocalTime.MIDNIGHT);
        salon.setTarifaBaseHora(new BigDecimal("100.00"));
        salon.setCostoPorPersona(new BigDecimal("1.00"));

        SesionActual.iniciar(cliente);
        BusinessValidationException permiso = assertThrows(BusinessValidationException.class,
            () -> service.actualizarDelProveedorActual(salon, List.of(tipoEventoId), List.of("LUNES")));
        assertTrue(permiso.getMessage().contains("permisos"));

        SesionActual.iniciar(new UsuarioDAOImpl().buscarPorEmail(proveedorEmail).orElseThrow());
        assertThrows(BusinessValidationException.class,
            () -> service.actualizarDelProveedorActual(salon, List.of(tipoEventoId), List.of("LUNES")));
    }

    @Test
    void proveedorNoPuedeCrearServiciosEnUnSalonAjeno() throws Exception {
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            otroProveedorId = crearUsuario(
                connection, Rol.ROLE_PROVEEDOR, "other-provider-" + unique + "@example.test"
            );
            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO establecimientos
                    (proveedor_id, zona_id, cuit, nombre_comercial, direccion, activo)
                VALUES (?, ?, ?, ?, 'Dirección ajena', TRUE)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setLong(1, otroProveedorId);
                statement.setLong(2, zonaId);
                statement.setString(3, "21" + unique.substring(0, 9));
                statement.setString(4, "Establecimiento ajeno " + unique);
                statement.executeUpdate();
                otroEstablecimientoId = generatedId(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO salones
                    (establecimiento_id, nombre_salon, capacidad_minima, capacidad_maxima,
                     hora_apertura, hora_cierre, tarifa_base_hora, costo_por_persona, activo)
                VALUES (?, ?, 1, 200, '00:00:00', '00:00:00', 100.00, 1.00, TRUE)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setLong(1, otroEstablecimientoId);
                statement.setString(2, "Salón ajeno " + unique);
                statement.executeUpdate();
                otroSalonId = generatedId(statement);
            }
        }
        SesionActual.iniciar(new UsuarioDAOImpl().buscarPorEmail(proveedorEmail).orElseThrow());
        ServicioAdicional servicio = new ServicioAdicional();
        servicio.setSalonId(otroSalonId);
        servicio.setNombre("Servicio no autorizado " + unique);
        servicio.setPrecio(new BigDecimal("10.00"));

        DatabaseException error = assertThrows(DatabaseException.class,
            () -> new ServicioAdicionalService(new com.mdp.eventos.dao.impl.ServicioAdicionalDAOImpl())
                .crearParaProveedor(servicio));

        assertTrue(error.getMessage().contains("no pertenece"));
    }

    @Test
    void noPermiteCambiarCapacidadQueAfectaUnaReservaFutura() throws Exception {
        long reservaId = insertarReserva(
            LocalDate.now().plusDays(3), LocalTime.of(18, 0), LocalTime.of(20, 0),
            "PENDIENTE", "bloqueo-" + unique, false, false
        );
        Usuario proveedor = new UsuarioDAOImpl().buscarPorEmail(proveedorEmail).orElseThrow();
        SesionActual.iniciar(proveedor);
        Salon salon = new Salon();
        salon.setId(salonId);
        salon.setEstablecimientoId(establecimientoId);
        salon.setNombreSalon("Salón test " + unique);
        salon.setCapacidadMinima(1);
        salon.setCapacidadMaxima(10);
        salon.setHoraApertura(LocalTime.MIDNIGHT);
        salon.setHoraCierre(LocalTime.MIDNIGHT);
        salon.setTarifaBaseHora(new BigDecimal("100.00"));
        salon.setCostoPorPersona(new BigDecimal("1.00"));

        BusinessValidationException error = assertThrows(BusinessValidationException.class,
            () -> new SalonService(new com.mdp.eventos.dao.impl.SalonDAOImpl())
                .actualizarDelProveedorActual(salon, List.of(tipoEventoId), List.of("LUNES")));

        assertTrue(error.getMessage().contains("bloqueo-" + unique));
        assertEquals("PENDIENTE", estado(reservaId));
    }

    private boolean intentarReservar(CountDownLatch inicio, LocalDate fecha, String suffix) throws Exception {
        inicio.await();
        Reserva reserva = new Reserva();
        reserva.setCodigoReserva("T" + unique.substring(0, 12) + suffix);
        reserva.setClienteId(clienteId);
        reserva.setSalonId(salonId);
        reserva.setTipoEventoId(tipoEventoId);
        reserva.setFechaEvento(fecha);
        reserva.setHoraInicio(LocalTime.of(22, 0));
        reserva.setHoraFin(LocalTime.of(2, 0));
        reserva.setCantidadInvitados(20);
        reserva.setMontoTotal(new BigDecimal("420.00"));
        reserva.setEstado(EstadoReserva.PENDIENTE);
        try {
            new ReservaDAOImpl().crear(reserva);
            return true;
        } catch (ScheduleConflictException expected) {
            return false;
        }
    }

    private long insertarReserva(
        LocalDate fecha, LocalTime inicio, LocalTime fin, String estado, String codigo,
        boolean vencida, boolean eventoFinalizado
    ) throws Exception {
        String sql = """
            INSERT INTO reservas
                (codigo_reserva, cliente_id, salon_id, tipo_evento_id, fecha_evento, hora_inicio,
                 hora_fin, cantidad_invitados, monto_total, estado, vence_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, 20, 420.00, ?,
                    IF(?, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE),
                        DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 48 HOUR)))
            """;
        LocalDate fechaReserva = eventoFinalizado ? LocalDate.now().minusDays(2) : fecha;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, codigo);
            statement.setLong(2, clienteId);
            statement.setLong(3, salonId);
            statement.setLong(4, tipoEventoId);
            statement.setObject(5, fechaReserva);
            statement.setObject(6, inicio);
            statement.setObject(7, fin);
            statement.setString(8, estado);
            statement.setBoolean(9, vencida);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private long crearUsuario(Connection connection, String rol, String email) throws SQLException {
        String sql = """
            INSERT INTO usuarios (rol_id, nombre_completo, email, password_hash, activo)
            SELECT id, ?, ?, 'test-only', TRUE FROM roles WHERE nombre = ?
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "Prueba " + rol);
            statement.setString(2, email);
            statement.setString(3, rol);
            assertEquals(1, statement.executeUpdate(), "Debe existir el rol " + rol + ".");
            return generatedId(statement);
        }
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("No se obtuvo una clave generada.");
            return keys.getLong(1);
        }
    }

    private String estado(long reservaId) throws Exception {
        return textoReserva(reservaId, "estado");
    }

    private String motivoCancelacion(long reservaId) throws Exception {
        return textoReserva(reservaId, "motivo_cancelacion");
    }

    private String textoReserva(long reservaId, String column) throws Exception {
        String sql = switch (column) {
            case "estado" -> "SELECT estado FROM reservas WHERE id = ?";
            case "motivo_cancelacion" -> "SELECT motivo_cancelacion FROM reservas WHERE id = ?";
            default -> throw new IllegalArgumentException("Campo de reserva no permitido.");
        };
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservaId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("No se encontró la reserva de prueba.");
                return row.getString(1);
            }
        }
    }

    private long canceladaPor(long reservaId) throws Exception {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT cancelada_por_usuario_id FROM reservas WHERE id = ?"
             )) {
            statement.setLong(1, reservaId);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private boolean canceladaEnRegistrada(long reservaId) throws Exception {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT cancelada_en IS NOT NULL FROM reservas WHERE id = ?"
             )) {
            statement.setLong(1, reservaId);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                return row.getBoolean(1);
            }
        }
    }

    private boolean requiereRevision(long reservaId) throws Exception {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT requiere_revision_admin FROM reservas WHERE id = ?"
             )) {
            statement.setLong(1, reservaId);
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                return row.getBoolean(1);
            }
        }
    }

    private void ejecutar(Connection connection, String sql, long... ids) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.length; index++) statement.setLong(index + 1, ids[index]);
            statement.executeUpdate();
        }
    }
}
