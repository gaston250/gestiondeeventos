package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SalonGestionDAOImpl {
    List<SalonProveedorDTO> listarPorProveedor(long proveedorId) throws DatabaseException {
        String sql = """
            SELECT s.id AS salon_id, s.establecimiento_id, s.nombre_salon, s.capacidad_minima,
                   s.capacidad_maxima, s.hora_apertura, s.hora_cierre, s.tarifa_base_hora,
                   s.costo_por_persona, s.activo, e.nombre_comercial,
                   ste.tipo_evento_id, ds.dia_semana
            FROM salones s
            JOIN establecimientos e ON e.id = s.establecimiento_id
            LEFT JOIN salon_tipo_evento ste ON ste.salon_id = s.id
            LEFT JOIN disponibilidad_semanal ds ON ds.salon_id = s.id AND ds.activo = TRUE
            WHERE e.proveedor_id = ?
            ORDER BY e.nombre_comercial, s.nombre_salon, ste.tipo_evento_id, ds.dia_semana
            """;
        Map<Long, Builder> salones = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, proveedorId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    long id = rows.getLong("salon_id");
                    Builder builder = salones.get(id);
                    if (builder == null) {
                        Salon salon = new Salon();
                        salon.setId(id);
                        salon.setEstablecimientoId(rows.getLong("establecimiento_id"));
                        salon.setNombreSalon(rows.getString("nombre_salon"));
                        salon.setCapacidadMinima(rows.getInt("capacidad_minima"));
                        salon.setCapacidadMaxima(rows.getInt("capacidad_maxima"));
                        salon.setHoraApertura(rows.getObject("hora_apertura", LocalTime.class));
                        salon.setHoraCierre(rows.getObject("hora_cierre", LocalTime.class));
                        salon.setTarifaBaseHora(rows.getBigDecimal("tarifa_base_hora"));
                        salon.setCostoPorPersona(rows.getBigDecimal("costo_por_persona"));
                        salon.setActivo(rows.getBoolean("activo"));
                        builder = new Builder(salon, rows.getString("nombre_comercial"));
                        salones.put(id, builder);
                    }
                    long typeId = rows.getLong("tipo_evento_id");
                    if (!rows.wasNull()) builder.tipos.add(typeId);
                    String day = rows.getString("dia_semana");
                    if (day != null) builder.dias.add(day);
                }
            }
            return salones.values().stream()
                .map(item -> new SalonProveedorDTO(
                    item.salon, item.establecimiento, List.copyOf(item.tipos), List.copyOf(item.dias)
                ))
                .toList();
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar los salones del proveedor.", exception);
        }
    }

    List<String> actualizarParaProveedor(
        Salon salon, long proveedorId, List<Long> tipos, List<String> dias
    ) throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                bloquearSalonPropio(connection, salon.getId(), proveedorId);
                List<String> conflictos = buscarReservasInconsistentes(connection, salon, tipos, dias, false);
                if (!conflictos.isEmpty()) {
                    connection.rollback();
                    return conflictos;
                }
                try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE salones SET nombre_salon = ?, capacidad_minima = ?, capacidad_maxima = ?,
                        hora_apertura = ?, hora_cierre = ?, tarifa_base_hora = ?, costo_por_persona = ?
                    WHERE id = ?
                    """)) {
                    update.setString(1, salon.getNombreSalon());
                    update.setInt(2, salon.getCapacidadMinima());
                    update.setInt(3, salon.getCapacidadMaxima());
                    update.setObject(4, salon.getHoraApertura());
                    update.setObject(5, salon.getHoraCierre());
                    update.setBigDecimal(6, salon.getTarifaBaseHora());
                    update.setBigDecimal(7, salon.getCostoPorPersona());
                    update.setLong(8, salon.getId());
                    update.executeUpdate();
                }
                reemplazarTiposYDias(connection, salon.getId(), tipos, dias);
                connection.commit();
                return List.of();
            } catch (SQLException | DatabaseException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ya existe un salón con ese nombre en el establecimiento.", exception);
            }
            throw new DatabaseException("No se pudo actualizar el salón.", exception);
        }
    }

    List<String> cambiarEstadoParaProveedor(long salonId, long proveedorId, boolean activo)
        throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                Salon salon = bloquearSalonPropio(connection, salonId, proveedorId);
                List<String> conflictos = activo
                    ? List.of()
                    : buscarReservasInconsistentes(connection, salon, null, null, true);
                if (!conflictos.isEmpty()) {
                    connection.rollback();
                    return conflictos;
                }
                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE salones SET activo = ? WHERE id = ?"
                )) {
                    update.setBoolean(1, activo);
                    update.setLong(2, salonId);
                    update.executeUpdate();
                }
                connection.commit();
                return List.of();
            } catch (SQLException | DatabaseException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cambiar el estado del salón.", exception);
        }
    }

    private Salon bloquearSalonPropio(Connection connection, long salonId, long proveedorId)
        throws SQLException, DatabaseException {
        String sql = """
            SELECT s.id, s.establecimiento_id, s.nombre_salon, s.capacidad_minima, s.capacidad_maxima,
                   s.hora_apertura, s.hora_cierre, s.tarifa_base_hora, s.costo_por_persona, s.activo
            FROM salones s
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE s.id = ? AND e.proveedor_id = ?
            FOR UPDATE
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salonId);
            statement.setLong(2, proveedorId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new DatabaseException("El salón no existe o no pertenece a tu cuenta.");
                Salon salon = new Salon();
                salon.setId(row.getLong("id"));
                salon.setEstablecimientoId(row.getLong("establecimiento_id"));
                salon.setNombreSalon(row.getString("nombre_salon"));
                salon.setCapacidadMinima(row.getInt("capacidad_minima"));
                salon.setCapacidadMaxima(row.getInt("capacidad_maxima"));
                salon.setHoraApertura(row.getObject("hora_apertura", LocalTime.class));
                salon.setHoraCierre(row.getObject("hora_cierre", LocalTime.class));
                salon.setTarifaBaseHora(row.getBigDecimal("tarifa_base_hora"));
                salon.setCostoPorPersona(row.getBigDecimal("costo_por_persona"));
                salon.setActivo(row.getBoolean("activo"));
                return salon;
            }
        }
    }

    private List<String> buscarReservasInconsistentes(
        Connection connection, Salon salon, List<Long> tipos, List<String> dias, boolean todas
    ) throws SQLException {
        String sql = """
            SELECT r.codigo_reserva, r.fecha_evento, r.hora_inicio, r.hora_fin,
                   r.cantidad_invitados, r.tipo_evento_id
            FROM reservas r
            WHERE r.salon_id = ? AND r.estado IN ('PENDIENTE','CONFIRMADA')
              AND TIMESTAMP(DATE_ADD(r.fecha_evento,
                  INTERVAL IF(r.hora_fin <= r.hora_inicio, 1, 0) DAY), r.hora_fin) > CURRENT_TIMESTAMP
            ORDER BY r.fecha_evento, r.hora_inicio
            FOR UPDATE
            """;
        List<String> conflictos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salon.getId());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    LocalDate fecha = rows.getObject("fecha_evento", LocalDate.class);
                    LocalTime inicio = rows.getObject("hora_inicio", LocalTime.class);
                    LocalTime fin = rows.getObject("hora_fin", LocalTime.class);
                    boolean rompeCapacidad = rows.getInt("cantidad_invitados") < salon.getCapacidadMinima()
                        || rows.getInt("cantidad_invitados") > salon.getCapacidadMaxima();
                    boolean rompeTipo = tipos != null && !tipos.contains(rows.getLong("tipo_evento_id"));
                    boolean rompeDia = dias != null && (!dias.contains(diaSemana(fecha))
                        || (HorarioEvento.cruzaMedianoche(inicio, fin)
                            && !dias.contains(diaSemana(fecha.plusDays(1)))));
                    boolean rompeHorario = dias != null && !HorarioEvento.estaDentroDeJornada(
                        salon.getHoraApertura(), salon.getHoraCierre(), inicio, fin
                    );
                    if (todas || rompeCapacidad || rompeTipo || rompeDia || rompeHorario) {
                        conflictos.add(rows.getString("codigo_reserva") + " — "
                            + fecha + " " + inicio + "–" + fin);
                    }
                }
            }
        }
        return List.copyOf(conflictos);
    }

    private void reemplazarTiposYDias(Connection connection, long salonId, List<Long> tipos, List<String> dias)
        throws SQLException {
        try (PreparedStatement deleteTypes = connection.prepareStatement(
                "DELETE FROM salon_tipo_evento WHERE salon_id = ?");
             PreparedStatement deleteDays = connection.prepareStatement(
                "DELETE FROM disponibilidad_semanal WHERE salon_id = ?");
             PreparedStatement insertType = connection.prepareStatement(
                "INSERT INTO salon_tipo_evento (salon_id, tipo_evento_id) SELECT ?, id FROM tipos_evento WHERE id = ? AND activo = TRUE");
             PreparedStatement insertDay = connection.prepareStatement(
                "INSERT INTO disponibilidad_semanal (salon_id, dia_semana, activo) VALUES (?, ?, TRUE)")) {
            deleteTypes.setLong(1, salonId);
            deleteTypes.executeUpdate();
            deleteDays.setLong(1, salonId);
            deleteDays.executeUpdate();
            for (Long typeId : tipos) {
                insertType.setLong(1, salonId);
                insertType.setLong(2, typeId);
                if (insertType.executeUpdate() != 1) throw new SQLException("Tipo de evento inexistente.");
            }
            for (String day : dias) {
                insertDay.setLong(1, salonId);
                insertDay.setString(2, day);
                insertDay.executeUpdate();
            }
        }
    }

    private String diaSemana(LocalDate fecha) {
        String nombre = fecha.getDayOfWeek().getDisplayName(
            java.time.format.TextStyle.FULL, Locale.forLanguageTag("es")
        );
        return Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            original.addSuppressed(exception);
        }
    }

    private static final class Builder {
        private final Salon salon;
        private final String establecimiento;
        private final Set<Long> tipos = new LinkedHashSet<>();
        private final Set<String> dias = new LinkedHashSet<>();

        private Builder(Salon salon, String establecimiento) {
            this.salon = salon;
            this.establecimiento = establecimiento;
        }
    }
}
