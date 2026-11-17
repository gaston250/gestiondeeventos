package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IServicioAdicionalDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.ServicioAdicional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServicioAdicionalDAOImpl implements IServicioAdicionalDAO {
    @Override
    public List<ServicioAdicional> listarPorSalon(long salonId) throws DatabaseException {
        String sql = """
            SELECT sa.id, e.id AS establecimiento_id, sa.salon_id, sa.nombre, sa.descripcion,
                   sa.precio, sa.cobro_por_invitado, sa.activo
            FROM servicios_adicionales sa
            JOIN salones s ON s.id = sa.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE sa.salon_id = ? AND sa.activo = TRUE
            ORDER BY sa.nombre
            """;
        List<ServicioAdicional> servicios = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ServicioAdicional servicio = new ServicioAdicional();
                    servicio.setId(resultSet.getLong("id"));
                    servicio.setEstablecimientoId(resultSet.getLong("establecimiento_id"));
                    servicio.setSalonId(resultSet.getLong("salon_id"));
                    servicio.setNombre(resultSet.getString("nombre"));
                    servicio.setDescripcion(resultSet.getString("descripcion"));
                    servicio.setPrecio(resultSet.getBigDecimal("precio"));
                    servicio.setCobroPorInvitado(resultSet.getBoolean("cobro_por_invitado"));
                    servicio.setActivo(resultSet.getBoolean("activo"));
                    servicios.add(servicio);
                }
            }
            return List.copyOf(servicios);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar los servicios adicionales del salón.", exception);
        }
    }

    @Override
    public List<ServicioAdicional> listarPorSalonProveedor(long salonId, long proveedorId)
        throws DatabaseException {
        String sql = """
            SELECT sa.id, e.id AS establecimiento_id, sa.salon_id, sa.nombre, sa.descripcion,
                   sa.precio, sa.cobro_por_invitado, sa.activo
            FROM servicios_adicionales sa
            JOIN salones s ON s.id = sa.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE s.id = ? AND e.proveedor_id = ?
            ORDER BY sa.nombre
            """;
        List<ServicioAdicional> servicios = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salonId);
            statement.setLong(2, proveedorId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) servicios.add(mapear(rows));
            }
            return List.copyOf(servicios);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar los servicios del salón.", exception);
        }
    }

    @Override
    public void crearParaSalon(ServicioAdicional servicio, long proveedorId) throws DatabaseException {
        String sql = """
            INSERT INTO servicios_adicionales
                (salon_id, nombre, descripcion, precio, cobro_por_invitado, activo)
            SELECT s.id, ?, ?, ?, ?, TRUE
            FROM salones s
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE s.id = ? AND e.proveedor_id = ?
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, servicio.getNombre());
            statement.setString(2, servicio.getDescripcion());
            statement.setBigDecimal(3, servicio.getPrecio());
            statement.setBoolean(4, servicio.isCobroPorInvitado());
            statement.setLong(5, servicio.getSalonId());
            statement.setLong(6, proveedorId);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("El salón no existe o no pertenece a tu cuenta.");
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ya existe un servicio con ese nombre en este salón.", exception);
            }
            throw new DatabaseException("No se pudo guardar el servicio adicional.", exception);
        }
    }

    @Override
    public boolean actualizarParaSalon(ServicioAdicional servicio, long proveedorId) throws DatabaseException {
        String sql = """
            UPDATE servicios_adicionales sa
            JOIN salones s ON s.id = sa.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            SET sa.nombre = ?, sa.descripcion = ?, sa.precio = ?, sa.cobro_por_invitado = ?
            WHERE sa.id = ? AND s.id = ? AND e.proveedor_id = ?
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, servicio.getNombre());
            statement.setString(2, servicio.getDescripcion());
            statement.setBigDecimal(3, servicio.getPrecio());
            statement.setBoolean(4, servicio.isCobroPorInvitado());
            statement.setLong(5, servicio.getId());
            statement.setLong(6, servicio.getSalonId());
            statement.setLong(7, proveedorId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ya existe un servicio con ese nombre en este salón.", exception);
            }
            throw new DatabaseException("No se pudo actualizar el servicio adicional.", exception);
        }
    }

    @Override
    public boolean cambiarEstadoParaSalon(long servicioId, long salonId, long proveedorId, boolean activo)
        throws DatabaseException {
        String sql = """
            UPDATE servicios_adicionales sa
            JOIN salones s ON s.id = sa.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            SET sa.activo = ?
            WHERE sa.id = ? AND s.id = ? AND e.proveedor_id = ?
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, activo);
            statement.setLong(2, servicioId);
            statement.setLong(3, salonId);
            statement.setLong(4, proveedorId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cambiar el estado del servicio adicional.", exception);
        }
    }

    @Override
    public List<ServicioAdicional> listarPlantillasActivas() throws DatabaseException {
        String sql = """
            SELECT id, nombre, descripcion, precio, cobro_por_invitado
            FROM servicios_base
            WHERE activo = TRUE
            ORDER BY nombre
            """;
        List<ServicioAdicional> plantillas = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                ServicioAdicional plantilla = new ServicioAdicional();
                plantilla.setId(rows.getLong("id"));
                plantilla.setNombre(rows.getString("nombre"));
                plantilla.setDescripcion(rows.getString("descripcion"));
                plantilla.setPrecio(rows.getBigDecimal("precio"));
                plantilla.setCobroPorInvitado(rows.getBoolean("cobro_por_invitado"));
                plantillas.add(plantilla);
            }
            return List.copyOf(plantillas);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar las plantillas de servicios.", exception);
        }
    }

    @Override
    public void crearDesdePlantilla(long plantillaId, long salonId, long proveedorId) throws DatabaseException {
        String sql = """
            INSERT INTO servicios_adicionales
                (salon_id, nombre, descripcion, precio, cobro_por_invitado, activo)
            SELECT s.id, base.nombre, base.descripcion, base.precio, base.cobro_por_invitado, TRUE
            FROM servicios_base base
            JOIN salones s ON s.id = ?
            JOIN establecimientos e ON e.id = s.establecimiento_id
            WHERE base.id = ? AND base.activo = TRUE AND e.proveedor_id = ?
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, salonId);
            statement.setLong(2, plantillaId);
            statement.setLong(3, proveedorId);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("La plantilla no está disponible o el salón no te pertenece.");
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ya existe en este salón un servicio con el nombre de la plantilla.", exception);
            }
            throw new DatabaseException("No se pudo copiar la plantilla al salón.", exception);
        }
    }

    private ServicioAdicional mapear(ResultSet row) throws SQLException {
        ServicioAdicional servicio = new ServicioAdicional();
        servicio.setId(row.getLong("id"));
        servicio.setEstablecimientoId(row.getLong("establecimiento_id"));
        servicio.setSalonId(row.getLong("salon_id"));
        servicio.setNombre(row.getString("nombre"));
        servicio.setDescripcion(row.getString("descripcion"));
        servicio.setPrecio(row.getBigDecimal("precio"));
        servicio.setCobroPorInvitado(row.getBoolean("cobro_por_invitado"));
        servicio.setActivo(row.getBoolean("activo"));
        return servicio;
    }
}
