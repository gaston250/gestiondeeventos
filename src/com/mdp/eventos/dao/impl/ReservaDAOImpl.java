package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IReservaDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.model.DetalleReservaServicio;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.dto.ResumenReservaDTO;
import com.mdp.eventos.dto.ReservaProveedorDTO;
import com.mdp.eventos.service.HorarioEvento;

import java.text.Normalizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReservaDAOImpl implements IReservaDAO {
    @Override
    public CondicionesSalon buscarCondicionesSalon(
        long salonId,
        int tipoEventoId,
        LocalDate fecha,
        java.time.LocalTime horaInicio,
        java.time.LocalTime horaFin
    )
        throws DatabaseException {
        String sql = """
            SELECT s.capacidad_minima, s.capacidad_maxima, s.hora_apertura, s.hora_cierre,
                   s.tarifa_base_hora, s.costo_por_persona
            FROM salones s
            JOIN salon_tipo_evento ste ON ste.salon_id = s.id
            JOIN tipos_evento te ON te.id = ste.tipo_evento_id AND te.activo = TRUE
            WHERE s.id = ? AND ste.tipo_evento_id = ?
              AND (
                  NOT EXISTS (SELECT 1 FROM disponibilidad_semanal ds WHERE ds.salon_id = s.id)
                  OR EXISTS (
                      SELECT 1 FROM disponibilidad_semanal ds
                      WHERE ds.salon_id = s.id AND ds.dia_semana = ? AND ds.activo = TRUE
                  )
              )
              AND (
                  ? = 0
                  OR NOT EXISTS (SELECT 1 FROM disponibilidad_semanal ds WHERE ds.salon_id = s.id)
                  OR EXISTS (
                      SELECT 1 FROM disponibilidad_semanal ds
                      WHERE ds.salon_id = s.id AND ds.dia_semana = ? AND ds.activo = TRUE
                  )
              )
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salonId);
            statement.setInt(2, tipoEventoId);
            statement.setString(3, diaSemana(fecha));
            boolean cruzaMedianoche = HorarioEvento.cruzaMedianoche(horaInicio, horaFin);
            statement.setBoolean(4, cruzaMedianoche);
            statement.setString(5, diaSemana(fecha.plusDays(1)));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                return new CondicionesSalon(
                    resultSet.getInt("capacidad_minima"),
                    resultSet.getInt("capacidad_maxima"),
                    resultSet.getObject("hora_apertura", java.time.LocalTime.class),
                    resultSet.getObject("hora_cierre", java.time.LocalTime.class),
                    resultSet.getBigDecimal("tarifa_base_hora"),
                    resultSet.getBigDecimal("costo_por_persona")
                );
            }
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar las condiciones del salón.", exception);
        }
    }

    @Override
    public Reserva crear(Reserva reserva) throws DatabaseException, ScheduleConflictException {
        String lockSql = """
            SELECT s.tarifa_base_hora, s.costo_por_persona
            FROM salones s
            JOIN establecimientos e ON e.id = s.establecimiento_id
            JOIN usuarios p ON p.id = e.proveedor_id
            WHERE s.id = ? AND s.activo = TRUE AND e.activo = TRUE AND p.activo = TRUE
              AND s.capacidad_minima <= ? AND s.capacidad_maxima >= ?
              AND EXISTS (
                  SELECT 1 FROM salon_tipo_evento ste
                  JOIN tipos_evento tipo ON tipo.id = ste.tipo_evento_id AND tipo.activo = TRUE
                  WHERE ste.salon_id = s.id AND ste.tipo_evento_id = ?
              )
              AND (
                  NOT EXISTS (SELECT 1 FROM disponibilidad_semanal ds WHERE ds.salon_id = s.id)
                  OR EXISTS (
                      SELECT 1 FROM disponibilidad_semanal ds
                      WHERE ds.salon_id = s.id AND ds.dia_semana = ? AND ds.activo = TRUE
                  )
              )
              AND (
                  ? = 0
                  OR NOT EXISTS (SELECT 1 FROM disponibilidad_semanal ds WHERE ds.salon_id = s.id)
                  OR EXISTS (
                      SELECT 1 FROM disponibilidad_semanal ds
                      WHERE ds.salon_id = s.id AND ds.dia_semana = ? AND ds.activo = TRUE
                  )
              )
              AND (
                  s.hora_cierre = s.hora_apertura
                  OR MOD(TIME_TO_SEC(?) - TIME_TO_SEC(s.hora_apertura) + 86400, 86400) + ? <=
                      MOD(TIME_TO_SEC(s.hora_cierre) - TIME_TO_SEC(s.hora_apertura) + 86400, 86400)
              )
            FOR UPDATE
            """;
        String conflictSql = """
            SELECT id FROM reservas
            WHERE salon_id = ?
              AND fecha_evento BETWEEN DATE_SUB(?, INTERVAL 1 DAY) AND ?
              AND estado IN ('PENDIENTE', 'CONFIRMADA')
              AND TIMESTAMP(fecha_evento, hora_inicio) <
                  TIMESTAMP(DATE_ADD(?, INTERVAL IF(? <= ?, 1, 0) DAY), ?)
              AND TIMESTAMP(
                  DATE_ADD(fecha_evento, INTERVAL IF(hora_fin <= hora_inicio, 1, 0) DAY),
                  hora_fin
              ) > TIMESTAMP(?, ?)
            LIMIT 1
            """;
        String insertSql = """
            INSERT INTO reservas (
                codigo_reserva, cliente_id, salon_id, tipo_evento_id, fecha_evento,
                hora_inicio, hora_fin, cantidad_invitados, monto_total, estado, vence_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR))
            """;
        String checkServiceSql = """
            SELECT sa.precio
            FROM servicios_adicionales sa
            WHERE sa.salon_id = ? AND sa.id = ? AND sa.activo = TRUE
            """;
        String insertServiceSql = """
            INSERT INTO detalle_reserva_servicios
                (reserva_id, servicio_id, cantidad, precio_unitario_pactado)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            BigDecimal tarifaBaseHora;
            BigDecimal costoPorPersona;
            actualizarEstadosAutomaticos(connection);
            try (PreparedStatement lock = connection.prepareStatement(lockSql)) {
                lock.setLong(1, reserva.getSalonId());
                lock.setInt(2, reserva.getCantidadInvitados());
                lock.setInt(3, reserva.getCantidadInvitados());
                lock.setLong(4, reserva.getTipoEventoId());
                lock.setString(5, diaSemana(reserva.getFechaEvento()));
                boolean cruzaMedianoche = HorarioEvento.cruzaMedianoche(
                    reserva.getHoraInicio(), reserva.getHoraFin()
                );
                lock.setBoolean(6, cruzaMedianoche);
                lock.setString(7, diaSemana(reserva.getFechaEvento().plusDays(1)));
                lock.setObject(8, reserva.getHoraInicio());
                lock.setLong(9, HorarioEvento.duracion(
                    reserva.getHoraInicio(), reserva.getHoraFin()
                ).getSeconds());
                try (ResultSet resultSet = lock.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new DatabaseException(
                            "El salón ya no está disponible, no acepta esos datos o cambió desde la cotización."
                        );
                    }
                    tarifaBaseHora = resultSet.getBigDecimal("tarifa_base_hora");
                    costoPorPersona = resultSet.getBigDecimal("costo_por_persona");
                }

                try (PreparedStatement conflict = connection.prepareStatement(conflictSql)) {
                    conflict.setLong(1, reserva.getSalonId());
                    conflict.setObject(2, reserva.getFechaEvento());
                    conflict.setObject(3, reserva.getFechaEvento());
                    conflict.setObject(4, reserva.getFechaEvento());
                    conflict.setObject(5, reserva.getHoraFin());
                    conflict.setObject(6, reserva.getHoraInicio());
                    conflict.setObject(7, reserva.getHoraFin());
                    conflict.setObject(8, reserva.getFechaEvento());
                    conflict.setObject(9, reserva.getHoraInicio());
                    try (ResultSet resultSet = conflict.executeQuery()) {
                        if (resultSet.next()) {
                            throw new ScheduleConflictException(
                                "Ese horario acaba de ocuparse. Elige otra fecha u horario y vuelve a buscar."
                            );
                        }
                    }
                }
                BigDecimal horas = BigDecimal.valueOf(HorarioEvento.duracion(
                        reserva.getHoraInicio(), reserva.getHoraFin()
                    ).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
                BigDecimal montoVerificado = tarifaBaseHora.multiply(horas)
                    .add(costoPorPersona.multiply(BigDecimal.valueOf(reserva.getCantidadInvitados())));
                try (PreparedStatement insert = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, reserva.getCodigoReserva());
                    insert.setLong(2, reserva.getClienteId());
                    insert.setLong(3, reserva.getSalonId());
                    insert.setLong(4, reserva.getTipoEventoId());
                    insert.setObject(5, reserva.getFechaEvento());
                    insert.setObject(6, reserva.getHoraInicio());
                    insert.setObject(7, reserva.getHoraFin());
                    insert.setInt(8, reserva.getCantidadInvitados());
                    insert.setBigDecimal(9, reserva.getMontoTotal());
                    insert.setString(10, reserva.getEstado().name());
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("MySQL no devolvió el id de la reserva insertada.");
                        reserva.setId(keys.getLong(1));
                    }
                }
                for (DetalleReservaServicio detalle : reserva.getServiciosAdicionales()) {
                    try (PreparedStatement serviceCheck = connection.prepareStatement(checkServiceSql)) {
                        serviceCheck.setLong(1, reserva.getSalonId());
                        serviceCheck.setLong(2, detalle.getServicioId());
                        try (ResultSet service = serviceCheck.executeQuery()) {
                            if (!service.next()) {
                                throw new DatabaseException(
                                    "Un servicio adicional ya no pertenece al establecimiento del salón."
                                );
                            }
                            BigDecimal currentPrice = service.getBigDecimal("precio");
                            if (currentPrice.compareTo(detalle.getPrecioUnitarioPactado()) != 0) {
                                throw new DatabaseException(
                                    "El precio de un servicio adicional cambió; actualice la cotización."
                                );
                            }
                            montoVerificado = montoVerificado.add(
                                currentPrice.multiply(BigDecimal.valueOf(detalle.getCantidad()))
                            );
                        }
                    }
                    try (PreparedStatement insertService = connection.prepareStatement(insertServiceSql)) {
                        insertService.setLong(1, reserva.getId());
                        insertService.setLong(2, detalle.getServicioId());
                        insertService.setInt(3, detalle.getCantidad());
                        insertService.setBigDecimal(4, detalle.getPrecioUnitarioPactado());
                        insertService.executeUpdate();
                    }
                }
                montoVerificado = montoVerificado.setScale(2, RoundingMode.HALF_UP);
                if (montoVerificado.compareTo(reserva.getMontoTotal()) != 0) {
                    throw new DatabaseException(
                        "El precio del salón cambió desde que se mostró la cotización. Vuelva a buscar y confirme el nuevo total."
                    );
                }
                connection.commit();
                return reserva;
            } catch (ScheduleConflictException | SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (DatabaseException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (ScheduleConflictException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo crear la reserva; se revirtieron los cambios.", exception);
        }
    }

    @Override
    public List<ResumenReservaDTO> listarPorCliente(
        long clienteId,
        EstadoReserva estado,
        LocalDate desde,
        LocalDate hasta
    ) throws DatabaseException {
        StringBuilder sql = new StringBuilder("""
            SELECT r.id, r.codigo_reserva, e.nombre_comercial AS establecimiento,
                   s.nombre_salon AS salon, r.fecha_evento, r.hora_inicio, r.hora_fin,
                   r.cantidad_invitados, r.monto_total, r.estado,
                   CASE WHEN r.estado = 'RECHAZADA' THEN r.motivo_rechazo
                        WHEN r.estado = 'CANCELADA' THEN CONCAT(
                            COALESCE(NULLIF(r.motivo_cancelacion, ''), 'Sin motivo informado'),
                            ' | Canceló: ', COALESCE(
                                (SELECT u.nombre_completo FROM usuarios u WHERE u.id = r.cancelada_por_usuario_id),
                                'Usuario'
                            ),
                            ' | ', COALESCE(DATE_FORMAT(r.cancelada_en, '%d/%m/%Y %H:%i'), 'Fecha no registrada')
                        )
                        WHEN r.estado = 'VENCIDA' THEN 'La solicitud venció sin respuesta dentro de 24 horas.'
                        ELSE NULL END AS motivo_estado
            FROM reservas r
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE r.cliente_id = ?
            """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(clienteId);
        if (estado != null) {
            sql.append(" AND r.estado = ?");
            parameters.add(estado.name());
        }
        if (desde != null) {
            sql.append(" AND r.fecha_evento >= ?");
            parameters.add(desde);
        }
        if (hasta != null) {
            sql.append(" AND r.fecha_evento <= ?");
            parameters.add(hasta);
        }
        sql.append(" ORDER BY r.fecha_evento DESC, r.hora_inicio DESC, r.id DESC");

        List<ResumenReservaDTO> reservas = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            actualizarEstadosAutomaticos(connection);
            for (int index = 0; index < parameters.size(); index++) {
                Object parameter = parameters.get(index);
                if (parameter instanceof String text) {
                    statement.setString(index + 1, text);
                } else {
                    statement.setObject(index + 1, parameter);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(new ResumenReservaDTO(
                        resultSet.getLong("id"),
                        resultSet.getString("codigo_reserva"),
                        resultSet.getString("establecimiento"),
                        resultSet.getString("salon"),
                        resultSet.getObject("fecha_evento", LocalDate.class),
                        resultSet.getObject("hora_inicio", LocalTime.class),
                        resultSet.getObject("hora_fin", LocalTime.class),
                        resultSet.getInt("cantidad_invitados"),
                        resultSet.getBigDecimal("monto_total"),
                        resultSet.getString("estado"),
                        resultSet.getString("motivo_estado")
                    ));
                }
            }
            return List.copyOf(reservas);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar las reservas del cliente.", exception);
        }
    }

    @Override
    public boolean cancelarAntesDelInicio(long reservaId, long clienteId) throws DatabaseException {
        String sql = """
            UPDATE reservas
            SET estado = 'CANCELADA', cancelada_por_usuario_id = ?, cancelada_en = CURRENT_TIMESTAMP
            WHERE id = ? AND cliente_id = ?
              AND estado IN ('PENDIENTE', 'CONFIRMADA')
              AND TIMESTAMP(fecha_evento, hora_inicio) > CURRENT_TIMESTAMP
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            actualizarEstadosAutomaticos(connection);
            statement.setLong(1, clienteId);
            statement.setLong(2, reservaId);
            statement.setLong(3, clienteId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cancelar la reserva.", exception);
        }
    }

    @Override
    public List<ReservaProveedorDTO> listarPorProveedor(long proveedorId) throws DatabaseException {
        String sql = """
            SELECT r.id, r.codigo_reserva, u.nombre_completo AS cliente, u.email AS email_cliente,
                   e.nombre_comercial AS establecimiento, s.nombre_salon AS salon, r.fecha_evento,
                   r.hora_inicio, r.hora_fin, r.cantidad_invitados, r.monto_total, r.estado,
                   CASE WHEN r.estado = 'RECHAZADA' THEN r.motivo_rechazo
                        WHEN r.estado = 'CANCELADA' THEN CONCAT(
                            COALESCE(NULLIF(r.motivo_cancelacion, ''), 'Sin motivo informado'),
                            ' | Canceló: ', COALESCE(
                                (SELECT u.nombre_completo FROM usuarios u WHERE u.id = r.cancelada_por_usuario_id),
                                'Usuario'
                            ),
                            ' | ', COALESCE(DATE_FORMAT(r.cancelada_en, '%d/%m/%Y %H:%i'), 'Fecha no registrada')
                        )
                        WHEN r.estado = 'VENCIDA' THEN 'La solicitud venció sin respuesta dentro de 24 horas.'
                        ELSE NULL END AS motivo_estado
            FROM reservas r
            JOIN usuarios u ON u.id = r.cliente_id
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE e.proveedor_id = ?
            ORDER BY CASE r.estado WHEN 'PENDIENTE' THEN 0 ELSE 1 END,
                     r.fecha_evento, r.hora_inicio, r.id
            """;
        List<ReservaProveedorDTO> reservas = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            actualizarEstadosAutomaticos(connection);
            statement.setLong(1, proveedorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(new ReservaProveedorDTO(
                        resultSet.getLong("id"),
                        resultSet.getString("codigo_reserva"),
                        resultSet.getString("cliente"),
                        resultSet.getString("email_cliente"),
                        resultSet.getString("establecimiento"),
                        resultSet.getString("salon"),
                        resultSet.getObject("fecha_evento", LocalDate.class),
                        resultSet.getObject("hora_inicio", LocalTime.class),
                        resultSet.getObject("hora_fin", LocalTime.class),
                        resultSet.getInt("cantidad_invitados"),
                        resultSet.getBigDecimal("monto_total"),
                        resultSet.getString("estado"),
                        resultSet.getString("motivo_estado")
                    ));
                }
            }
            return List.copyOf(reservas);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar las solicitudes de tus establecimientos.", exception);
        }
    }

    @Override
    public boolean actualizarEstadoPendiente(
        long reservaId,
        long proveedorId,
        EstadoReserva nuevoEstado,
        String motivo
    ) throws DatabaseException {
        String lockSql = """
            SELECT s.id
            FROM reservas r JOIN salones s ON s.id = r.salon_id
            WHERE r.id = ? FOR UPDATE
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            long salonId;
            try (PreparedStatement lock = connection.prepareStatement(lockSql)) {
                lock.setLong(1, reservaId);
                try (ResultSet row = lock.executeQuery()) {
                    if (!row.next()) {
                        connection.rollback();
                        return false;
                    }
                    salonId = row.getLong(1);
                }
            }
            actualizarEstadosAutomaticos(connection);
            if (nuevoEstado == EstadoReserva.CONFIRMADA
                && haySuperposicion(connection, salonId, reservaId)) {
                connection.rollback();
                return false;
            }
            String sql = """
                UPDATE reservas AS r
                JOIN salones s ON s.id = r.salon_id
                JOIN establecimientos e ON e.id = s.establecimiento_id
                SET r.estado = ?, r.motivo_rechazo = ?
                WHERE r.id = ? AND r.estado = 'PENDIENTE'
                  AND e.proveedor_id = ?
                  AND (? <> 'CONFIRMADA' OR TIMESTAMP(r.fecha_evento, r.hora_inicio) > CURRENT_TIMESTAMP)
                """;
            try (PreparedStatement update = connection.prepareStatement(sql)) {
                update.setString(1, nuevoEstado.name());
                update.setString(2, nuevoEstado == EstadoReserva.RECHAZADA ? motivo : null);
                update.setLong(3, reservaId);
                update.setLong(4, proveedorId);
                update.setString(5, nuevoEstado.name());
                int changed = update.executeUpdate();
                connection.commit();
                return changed == 1;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo actualizar el estado de la solicitud.", exception);
        }
    }

    @Override
    public boolean cancelarPorProveedor(long reservaId, long proveedorId, long actorId, String motivo)
        throws DatabaseException {
        return cancelarPorActor(reservaId, proveedorId, actorId, motivo, false);
    }

    @Override
    public boolean cancelarPorAdministrador(long reservaId, long actorId, String motivo)
        throws DatabaseException {
        return cancelarPorActor(reservaId, null, actorId, motivo, true);
    }

    private boolean cancelarPorActor(
        long reservaId,
        Long proveedorId,
        long actorId,
        String motivo,
        boolean administrador
    ) throws DatabaseException {
        String sql = """
            UPDATE reservas r
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            SET r.estado = 'CANCELADA', r.motivo_cancelacion = ?,
                r.requiere_revision_admin = FALSE,
                r.cancelada_por_usuario_id = ?, r.cancelada_en = CURRENT_TIMESTAMP
            WHERE r.id = ? AND r.estado IN ('PENDIENTE', 'CONFIRMADA')
              AND TIMESTAMP(r.fecha_evento, r.hora_inicio) > CURRENT_TIMESTAMP
              AND (? OR e.proveedor_id = ?)
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            actualizarEstadosAutomaticos(connection);
            statement.setString(1, motivo);
            statement.setLong(2, actorId);
            statement.setLong(3, reservaId);
            statement.setBoolean(4, administrador);
            if (proveedorId == null) statement.setNull(5, java.sql.Types.BIGINT);
            else statement.setLong(5, proveedorId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cancelar la reserva.", exception);
        }
    }

    private boolean haySuperposicion(Connection connection, long salonId, long reservaId) throws SQLException {
        String sql = """
            SELECT 1 FROM reservas otra
            JOIN reservas actual ON actual.id = ?
            WHERE otra.salon_id = ? AND otra.id <> ?
              AND otra.estado IN ('PENDIENTE', 'CONFIRMADA')
              AND TIMESTAMP(otra.fecha_evento, otra.hora_inicio) <
                  TIMESTAMP(DATE_ADD(actual.fecha_evento,
                      INTERVAL IF(actual.hora_fin <= actual.hora_inicio, 1, 0) DAY), actual.hora_fin)
              AND TIMESTAMP(DATE_ADD(otra.fecha_evento,
                      INTERVAL IF(otra.hora_fin <= otra.hora_inicio, 1, 0) DAY), otra.hora_fin) >
                  TIMESTAMP(actual.fecha_evento, actual.hora_inicio)
            LIMIT 1
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservaId);
            statement.setLong(2, salonId);
            statement.setLong(3, reservaId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        }
    }

    @Override
    public void actualizarEstadosAutomaticos() throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            actualizarEstadosAutomaticos(connection);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron actualizar los estados automáticos de las reservas.", exception);
        }
    }

    private void actualizarEstadosAutomaticos(Connection connection) throws SQLException {
        String vencer = """
            UPDATE reservas
            SET estado = 'VENCIDA'
            WHERE estado = 'PENDIENTE'
              AND (
                  vence_en <= CURRENT_TIMESTAMP
                  OR TIMESTAMP(fecha_evento, hora_inicio) <= CURRENT_TIMESTAMP
              )
            """;
        String finalizar = """
            UPDATE reservas
            SET estado = 'FINALIZADA', requiere_revision_admin = FALSE
            WHERE estado = 'CONFIRMADA'
              AND TIMESTAMP(DATE_ADD(fecha_evento,
                  INTERVAL IF(hora_fin <= hora_inicio, 1, 0) DAY), hora_fin) <= CURRENT_TIMESTAMP
            """;
        try (PreparedStatement statement = connection.prepareStatement(vencer)) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(finalizar)) {
            statement.executeUpdate();
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private String diaSemana(LocalDate fecha) {
        String nombreDia = fecha.getDayOfWeek()
            .getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("es"));
        return Normalizer.normalize(nombreDia, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
    }
}
