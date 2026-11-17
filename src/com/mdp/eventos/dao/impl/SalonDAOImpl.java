package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.ISalonDAO;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.service.HorarioEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.text.Normalizer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class SalonDAOImpl implements ISalonDAO {
    private final SalonGestionDAOImpl gestionDAO = new SalonGestionDAOImpl();
    private static final String SQL_CREAR_PARA_PROVEEDOR = """
        INSERT INTO salones (
            establecimiento_id, nombre_salon, capacidad_minima, capacidad_maxima,
            hora_apertura, hora_cierre, tarifa_base_hora, costo_por_persona
        )
        SELECT e.id, ?, ?, ?, ?, ?, ?, ?
        FROM establecimientos e
        WHERE e.id = ? AND e.proveedor_id = ? AND e.activo = TRUE
        """;
    private static final String SQL_BUSCAR_DISPONIBLES = """
        SELECT s.id, e.nombre_comercial AS establecimiento, s.nombre_salon AS salon,
               z.nombre AS zona, e.direccion, s.capacidad_minima, s.capacidad_maxima,
               s.tarifa_base_hora, s.costo_por_persona, s.hora_apertura, s.hora_cierre
        FROM salones s
        JOIN establecimientos e ON e.id = s.establecimiento_id
        JOIN zonas_mdp z ON z.id = e.zona_id
        JOIN usuarios proveedor ON proveedor.id = e.proveedor_id
        WHERE e.activo = TRUE
          AND proveedor.activo = TRUE
          AND s.activo = TRUE
          AND z.activo = TRUE
          AND s.capacidad_minima <= ?
          AND s.capacidad_maxima >= ?
          AND (
              s.hora_cierre = s.hora_apertura
              OR MOD(TIME_TO_SEC(?) - TIME_TO_SEC(s.hora_apertura) + 86400, 86400) + ? <=
                  MOD(TIME_TO_SEC(s.hora_cierre) - TIME_TO_SEC(s.hora_apertura) + 86400, 86400)
          )
          AND EXISTS (
              SELECT 1
              FROM salon_tipo_evento ste
              JOIN tipos_evento tipo ON tipo.id = ste.tipo_evento_id
              WHERE ste.salon_id = s.id
                AND ste.tipo_evento_id = ?
                AND tipo.activo = TRUE
          )
          AND (? IS NULL OR z.nombre = ?)
          AND (
              NOT EXISTS (
                  SELECT 1
                  FROM disponibilidad_semanal ds
                  WHERE ds.salon_id = s.id
              )
              OR EXISTS (
                  SELECT 1
                  FROM disponibilidad_semanal ds
                  WHERE ds.salon_id = s.id
                    AND ds.dia_semana = ?
                    AND ds.activo = TRUE
              )
          )
          AND (
              ? = 0
              OR NOT EXISTS (
                  SELECT 1
                  FROM disponibilidad_semanal ds
                  WHERE ds.salon_id = s.id
              )
              OR EXISTS (
                  SELECT 1
                  FROM disponibilidad_semanal ds
                  WHERE ds.salon_id = s.id
                    AND ds.dia_semana = ?
                    AND ds.activo = TRUE
              )
          )
          AND NOT EXISTS (
              SELECT 1
              FROM reservas r
              WHERE r.salon_id = s.id
                AND r.estado IN ('PENDIENTE', 'CONFIRMADA')
                AND r.fecha_evento BETWEEN DATE_SUB(?, INTERVAL 1 DAY) AND ?
                AND TIMESTAMP(r.fecha_evento, r.hora_inicio) <
                    TIMESTAMP(DATE_ADD(?, INTERVAL IF(? <= ?, 1, 0) DAY), ?)
                AND TIMESTAMP(
                    DATE_ADD(r.fecha_evento, INTERVAL IF(r.hora_fin <= r.hora_inicio, 1, 0) DAY),
                    r.hora_fin
                ) > TIMESTAMP(?, ?)
          )
        ORDER BY s.tarifa_base_hora, s.costo_por_persona, e.nombre_comercial, s.nombre_salon
        """;

    @Override
    public List<SalonDisponibilidadDTO> buscarDisponibles(
        int tipoEventoId,
        int invitados,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String zona
    ) throws DatabaseException {
        List<SalonDisponibilidadDTO> salones = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_BUSCAR_DISPONIBLES)) {
            actualizarEstadosAutomaticos(connection);
            statement.setInt(1, invitados);
            statement.setInt(2, invitados);
            statement.setObject(3, horaInicio);
            statement.setLong(4, duracionSegundos(horaInicio, horaFin));
            statement.setInt(5, tipoEventoId);
            if (zona == null || zona.isBlank()) {
                statement.setNull(6, java.sql.Types.VARCHAR);
                statement.setNull(7, java.sql.Types.VARCHAR);
            } else {
                statement.setString(6, zona);
                statement.setString(7, zona);
            }

            statement.setString(8, diaSemana(fecha));
            boolean cruzaMedianoche = HorarioEvento.cruzaMedianoche(horaInicio, horaFin);
            statement.setBoolean(9, cruzaMedianoche);
            statement.setString(10, diaSemana(fecha.plusDays(1)));
            statement.setObject(11, fecha);
            statement.setObject(12, fecha);
            statement.setObject(13, fecha);
            statement.setObject(14, horaFin);
            statement.setObject(15, horaInicio);
            statement.setObject(16, horaFin);
            statement.setObject(17, fecha);
            statement.setObject(18, horaInicio);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    salones.add(new SalonDisponibilidadDTO(
                        resultSet.getLong("id"),
                        resultSet.getString("establecimiento"),
                        resultSet.getString("salon"),
                        resultSet.getString("zona"),
                        resultSet.getString("direccion"),
                        resultSet.getInt("capacidad_minima"),
                        resultSet.getInt("capacidad_maxima"),
                        resultSet.getBigDecimal("tarifa_base_hora"),
                        resultSet.getBigDecimal("costo_por_persona"),
                        resultSet.getObject("hora_apertura", LocalTime.class),
                        resultSet.getObject("hora_cierre", LocalTime.class)
                    ));
                }
            }

            return List.copyOf(salones);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar los salones disponibles.", exception);
        }
    }

    private void actualizarEstadosAutomaticos(Connection connection) throws SQLException {
        try (PreparedStatement expire = connection.prepareStatement("""
            UPDATE reservas SET estado = 'VENCIDA'
            WHERE estado = 'PENDIENTE' AND vence_en <= CURRENT_TIMESTAMP
            """);
             PreparedStatement finish = connection.prepareStatement("""
            UPDATE reservas SET estado = 'FINALIZADA'
            WHERE estado = 'CONFIRMADA'
              AND TIMESTAMP(DATE_ADD(fecha_evento,
                  INTERVAL IF(hora_fin <= hora_inicio, 1, 0) DAY), hora_fin) <= CURRENT_TIMESTAMP
            """)) {
            expire.executeUpdate();
            finish.executeUpdate();
        }
    }

    @Override
    public void crearParaProveedor(
        Salon salon,
        long proveedorId,
        List<Long> tiposEvento,
        List<String> diasDisponibles
    ) throws DatabaseException {
        String insertEventType = """
            INSERT INTO salon_tipo_evento (salon_id, tipo_evento_id)
            SELECT ?, id FROM tipos_evento WHERE id = ? AND activo = TRUE
            """;
        String insertAvailability = """
            INSERT INTO disponibilidad_semanal (salon_id, dia_semana, activo)
            VALUES (?, ?, TRUE)
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement insert = connection.prepareStatement(
                    SQL_CREAR_PARA_PROVEEDOR, PreparedStatement.RETURN_GENERATED_KEYS
                )) {
                    insert.setString(1, salon.getNombreSalon());
                    insert.setInt(2, salon.getCapacidadMinima());
                    insert.setInt(3, salon.getCapacidadMaxima());
                    insert.setObject(4, salon.getHoraApertura());
                    insert.setObject(5, salon.getHoraCierre());
                    insert.setBigDecimal(6, salon.getTarifaBaseHora());
                    insert.setBigDecimal(7, salon.getCostoPorPersona());
                    insert.setLong(8, salon.getEstablecimientoId());
                    insert.setLong(9, proveedorId);
                    if (insert.executeUpdate() != 1) {
                        throw new DatabaseException(
                            "No se pudo agregar el salón: el establecimiento no existe, está inactivo "
                                + "o no pertenece a tu cuenta."
                        );
                    }
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("MySQL no devolvió el id del salón creado.");
                        salon.setId(keys.getLong(1));
                    }
                }

                try (PreparedStatement insertType = connection.prepareStatement(insertEventType)) {
                    for (Long typeId : tiposEvento) {
                        insertType.setLong(1, salon.getId());
                        insertType.setLong(2, typeId);
                        if (insertType.executeUpdate() != 1) {
                            throw new DatabaseException("Uno de los tipos de evento seleccionados ya no existe.");
                        }
                    }
                }

                try (PreparedStatement insertDay = connection.prepareStatement(insertAvailability)) {
                    for (String day : diasDisponibles) {
                        insertDay.setLong(1, salon.getId());
                        insertDay.setString(2, day);
                        insertDay.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | DatabaseException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException(
                    "No se pudo guardar el salón. Puede existir otro salón con ese nombre en el establecimiento.",
                    exception
                );
            }
            throw new DatabaseException("No se pudo guardar el salón en MySQL.", exception);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    @Override
    public List<SalonProveedorDTO> listarPorProveedor(long proveedorId) throws DatabaseException {
        return gestionDAO.listarPorProveedor(proveedorId);
    }

    @Override
    public List<String> actualizarParaProveedor(
        Salon salon, long proveedorId, List<Long> tiposEvento, List<String> diasDisponibles
    ) throws DatabaseException {
        return gestionDAO.actualizarParaProveedor(salon, proveedorId, tiposEvento, diasDisponibles);
    }

    @Override
    public List<String> cambiarEstadoParaProveedor(long salonId, long proveedorId, boolean activo)
        throws DatabaseException {
        return gestionDAO.cambiarEstadoParaProveedor(salonId, proveedorId, activo);
    }

    private long duracionSegundos(LocalTime inicio, LocalTime fin) {
        return Duration.between(inicio, fin).getSeconds() + (fin.isBefore(inicio) ? 86_400 : 0);
    }

    private String diaSemana(LocalDate fecha) {
        String nombreDia = fecha.getDayOfWeek()
            .getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("es"));
        return Normalizer.normalize(nombreDia, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
    }
}
