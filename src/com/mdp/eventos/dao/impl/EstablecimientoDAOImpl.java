package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IEstablecimientoDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Establecimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EstablecimientoDAOImpl implements IEstablecimientoDAO {
    private static final String SQL_CREAR = """
        INSERT INTO establecimientos
            (proveedor_id, zona_id, cuit, nombre_comercial, direccion, activo)
        SELECT ?, z.id, ?, ?, ?, TRUE
        FROM zonas_mdp z
        WHERE z.id = ? AND z.activo = TRUE
        """;
    private static final String SQL_LISTAR_POR_PROVEEDOR = """
        SELECT id, proveedor_id, zona_id, cuit, nombre_comercial, direccion, activo
        FROM establecimientos
        WHERE proveedor_id = ?
        ORDER BY nombre_comercial
        """;
    private static final String SQL_ACTUALIZAR = """
        UPDATE establecimientos e
        SET zona_id = ?, cuit = ?, nombre_comercial = ?, direccion = ?
        WHERE e.id = ? AND e.proveedor_id = ?
          AND EXISTS (SELECT 1 FROM zonas_mdp z WHERE z.id = ? AND z.activo = TRUE)
          AND NOT EXISTS (
              SELECT 1
              FROM salones s
              JOIN reservas r ON r.salon_id = s.id
              WHERE s.establecimiento_id = e.id
                AND r.estado = 'CONFIRMADA'
                AND TIMESTAMP(r.fecha_evento, r.hora_inicio) > CURRENT_TIMESTAMP
          )
        """;
    private static final String SQL_CAMBIAR_ESTADO = """
        UPDATE establecimientos e
        SET activo = ?
        WHERE e.id = ? AND e.proveedor_id = ?
          AND (
              ? = TRUE
              OR NOT EXISTS (
                  SELECT 1
                  FROM salones s
                  JOIN reservas r ON r.salon_id = s.id
                  WHERE s.establecimiento_id = e.id
                    AND r.estado = 'CONFIRMADA'
                    AND TIMESTAMP(r.fecha_evento, r.hora_inicio) > CURRENT_TIMESTAMP
              )
          )
        """;

    @Override
    public List<Establecimiento> listarPorProveedor(long proveedorId) throws DatabaseException {
        List<Establecimiento> establecimientos = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_LISTAR_POR_PROVEEDOR)) {
            statement.setLong(1, proveedorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Establecimiento establecimiento = new Establecimiento();
                    establecimiento.setId(resultSet.getLong("id"));
                    establecimiento.setProveedorId(resultSet.getLong("proveedor_id"));
                    establecimiento.setZonaId(resultSet.getLong("zona_id"));
                    establecimiento.setCuit(resultSet.getString("cuit"));
                    establecimiento.setNombreComercial(resultSet.getString("nombre_comercial"));
                    establecimiento.setDireccion(resultSet.getString("direccion"));
                    establecimiento.setActivo(resultSet.getBoolean("activo"));
                    establecimientos.add(establecimiento);
                }
            }
            return List.copyOf(establecimientos);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron consultar los establecimientos del proveedor.", exception);
        }
    }

    @Override
    public void crearParaProveedor(Establecimiento establecimiento) throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_CREAR)) {
            statement.setLong(1, establecimiento.getProveedorId());
            statement.setString(2, establecimiento.getCuit());
            statement.setString(3, establecimiento.getNombreComercial());
            statement.setString(4, establecimiento.getDireccion());
            statement.setLong(5, establecimiento.getZonaId());
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("La zona seleccionada no existe en la base de datos.");
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException(
                    "No se pudo registrar el establecimiento. Verifica que el CUIT no esté registrado.",
                    exception
                );
            }
            throw new DatabaseException("No se pudo guardar el establecimiento en MySQL.", exception);
        }
    }

    @Override
    public boolean actualizarParaProveedor(Establecimiento establecimiento, long proveedorId)
        throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_ACTUALIZAR)) {
            statement.setLong(1, establecimiento.getZonaId());
            statement.setString(2, establecimiento.getCuit());
            statement.setString(3, establecimiento.getNombreComercial());
            statement.setString(4, establecimiento.getDireccion());
            statement.setLong(5, establecimiento.getId());
            statement.setLong(6, proveedorId);
            statement.setLong(7, establecimiento.getZonaId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("El CUIT indicado ya está asociado a otro establecimiento.", exception);
            }
            throw new DatabaseException("No se pudo actualizar el establecimiento en MySQL.", exception);
        }
    }

    @Override
    public boolean cambiarEstadoParaProveedor(long establecimientoId, long proveedorId, boolean activo)
        throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_CAMBIAR_ESTADO)) {
            statement.setBoolean(1, activo);
            statement.setLong(2, establecimientoId);
            statement.setLong(3, proveedorId);
            statement.setBoolean(4, activo);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cambiar el estado del establecimiento.", exception);
        }
    }

}
