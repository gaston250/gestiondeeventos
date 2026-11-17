package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IAdminDAO;
import com.mdp.eventos.dto.ReservaAdminDTO;
import com.mdp.eventos.dto.ResumenAdminDTO;
import com.mdp.eventos.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class AdminDAOImpl implements IAdminDAO {
    private static final String SQL_RESUMEN = """
        SELECT
            (SELECT COUNT(*) FROM usuarios) AS usuarios_registrados,
            (SELECT COUNT(*) FROM establecimientos WHERE activo = TRUE) AS establecimientos_activos,
            (SELECT COUNT(*) FROM reservas WHERE estado = 'PENDIENTE') AS reservas_pendientes,
            (SELECT MAX(creado_en) FROM reservas) AS ultima_reserva
        """;
    private static final String SQL_RESERVAS = """
        SELECT r.id, r.codigo_reserva, u.nombre_completo AS cliente, u.email AS email_cliente,
               e.nombre_comercial AS establecimiento, s.nombre_salon AS salon, r.fecha_evento,
               r.hora_inicio, r.hora_fin, r.cantidad_invitados, r.monto_total, r.estado,
               CASE WHEN r.estado = 'RECHAZADA' THEN r.motivo_rechazo
                    WHEN r.estado = 'CONFIRMADA' AND r.requiere_revision_admin = TRUE
                        THEN 'Proveedor dado de baja; requiere revisión del administrador.'
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
        WHERE (? = '' OR LOCATE(LOWER(?), LOWER(CONCAT_WS(
            ' ', r.codigo_reserva, u.nombre_completo, u.email, e.nombre_comercial, s.nombre_salon
        ))) > 0)
          AND (? IS NULL OR r.estado = ?)
        ORDER BY r.creado_en DESC, r.id DESC
        LIMIT ? OFFSET ?
        """;

    @Override
    public ResumenAdminDTO obtenerResumen() throws DatabaseException {
        new ReservaDAOImpl().actualizarEstadosAutomaticos();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_RESUMEN);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new SQLException("MySQL no devolvió el resumen administrativo.");
            Timestamp latest = resultSet.getTimestamp("ultima_reserva");
            return new ResumenAdminDTO(
                resultSet.getLong("usuarios_registrados"),
                resultSet.getLong("establecimientos_activos"),
                resultSet.getLong("reservas_pendientes"),
                latest == null ? null : latest.toLocalDateTime()
            );
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar los indicadores del administrador.", exception);
        }
    }

    @Override
    public List<ReservaAdminDTO> listarReservas(int limite, int offset, String busqueda, String estado)
        throws DatabaseException {
        new ReservaDAOImpl().actualizarEstadosAutomaticos();
        List<ReservaAdminDTO> reservas = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_RESERVAS)) {
            statement.setString(1, busqueda);
            statement.setString(2, busqueda.toLowerCase(java.util.Locale.ROOT));
            if (estado == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, estado);
                statement.setString(4, estado);
            }
            statement.setInt(5, limite);
            statement.setInt(6, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reservas.add(new ReservaAdminDTO(
                    resultSet.getLong("id"),
                    resultSet.getString("codigo_reserva"),
                    resultSet.getString("cliente"),
                    resultSet.getString("email_cliente"),
                    resultSet.getString("establecimiento"),
                    resultSet.getString("salon"),
                    resultSet.getObject("fecha_evento", java.time.LocalDate.class),
                    resultSet.getObject("hora_inicio", java.time.LocalTime.class),
                    resultSet.getObject("hora_fin", java.time.LocalTime.class),
                    resultSet.getInt("cantidad_invitados"),
                    resultSet.getBigDecimal("monto_total"),
                    resultSet.getString("estado"),
                    resultSet.getString("motivo_estado")
                ));
            }
            }
            return List.copyOf(reservas);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar las reservas para el administrador.", exception);
        }
    }

    @Override
    public long contarReservas(String busqueda, String estado) throws DatabaseException {
        String sql = """
            SELECT COUNT(*)
            FROM reservas r
            JOIN usuarios u ON u.id = r.cliente_id
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE (? = '' OR LOCATE(LOWER(?), LOWER(CONCAT_WS(
                ' ', r.codigo_reserva, u.nombre_completo, u.email, e.nombre_comercial, s.nombre_salon
            ))) > 0)
              AND (? IS NULL OR r.estado = ?)
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ) {
            statement.setString(1, busqueda);
            statement.setString(2, busqueda.toLowerCase(java.util.Locale.ROOT));
            if (estado == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, estado);
                statement.setString(4, estado);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new SQLException("MySQL no devolvió el total de reservas.");
            return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo contar las reservas.", exception);
        }
    }

    @Override
    public boolean cancelarReserva(long reservaId, long administradorId, String motivo) throws DatabaseException {
        return new ReservaDAOImpl().cancelarPorAdministrador(reservaId, administradorId, motivo);
    }
}
