package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.ICatalogoAdminDAO;
import com.mdp.eventos.dto.CatalogoAdminDTO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.TipoCatalogoAdmin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CatalogoAdminDAOImpl implements ICatalogoAdminDAO {
    @Override
    public List<CatalogoAdminDTO> listar(TipoCatalogoAdmin tipo) throws DatabaseException {
        String sql = switch (tipo) {
            case ZONA -> """
                SELECT id, nombre, NULL AS descripcion, codigo_postal, NULL AS precio,
                       FALSE AS cobro_por_invitado, activo
                FROM zonas_mdp ORDER BY nombre
                """;
            case TIPO_EVENTO -> """
                SELECT id, nombre, descripcion, NULL AS codigo_postal, NULL AS precio,
                       FALSE AS cobro_por_invitado, activo
                FROM tipos_evento ORDER BY nombre
                """;
            case SERVICIO_BASE -> """
                SELECT id, nombre, descripcion, NULL AS codigo_postal, precio,
                       cobro_por_invitado, activo
                FROM servicios_base ORDER BY nombre
                """;
        };
        List<CatalogoAdminDTO> catalogos = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                catalogos.add(new CatalogoAdminDTO(
                    rows.getLong("id"), tipo, rows.getString("nombre"), rows.getString("descripcion"),
                    rows.getString("codigo_postal"), rows.getBigDecimal("precio"),
                    rows.getBoolean("cobro_por_invitado"), rows.getBoolean("activo")
                ));
            }
            return List.copyOf(catalogos);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar los catálogos administrativos.", exception);
        }
    }

    @Override
    public void crear(CatalogoAdminDTO catalogo) throws DatabaseException {
        String sql = switch (catalogo.tipo()) {
            case ZONA -> "INSERT INTO zonas_mdp (nombre, codigo_postal, activo) VALUES (?, ?, TRUE)";
            case TIPO_EVENTO -> "INSERT INTO tipos_evento (nombre, descripcion, activo) VALUES (?, ?, TRUE)";
            case SERVICIO_BASE -> """
                INSERT INTO servicios_base (nombre, descripcion, precio, cobro_por_invitado, activo)
                VALUES (?, ?, ?, ?, TRUE)
                """;
        };
        ejecutar(catalogo, sql, false);
    }

    @Override
    public boolean actualizar(CatalogoAdminDTO catalogo) throws DatabaseException {
        String sql = switch (catalogo.tipo()) {
            case ZONA -> "UPDATE zonas_mdp SET nombre = ?, codigo_postal = ? WHERE id = ?";
            case TIPO_EVENTO -> "UPDATE tipos_evento SET nombre = ?, descripcion = ? WHERE id = ?";
            case SERVICIO_BASE -> """
                UPDATE servicios_base SET nombre = ?, descripcion = ?, precio = ?, cobro_por_invitado = ?
                WHERE id = ?
                """;
        };
        return ejecutar(catalogo, sql, true) == 1;
    }

    @Override
    public boolean cambiarEstado(TipoCatalogoAdmin tipo, long id, boolean activo) throws DatabaseException {
        String sql = switch (tipo) {
            case ZONA -> "UPDATE zonas_mdp SET activo = ? WHERE id = ?";
            case TIPO_EVENTO -> "UPDATE tipos_evento SET activo = ? WHERE id = ?";
            case SERVICIO_BASE -> "UPDATE servicios_base SET activo = ? WHERE id = ?";
        };
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, activo);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cambiar el estado del catálogo.", exception);
        }
    }

    private int ejecutar(CatalogoAdminDTO dto, String sql, boolean actualizar) throws DatabaseException {
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, dto.nombre());
            switch (dto.tipo()) {
                case ZONA -> statement.setString(index++, dto.codigoPostal());
                case TIPO_EVENTO -> statement.setString(index++, dto.descripcion());
                case SERVICIO_BASE -> {
                    statement.setString(index++, dto.descripcion());
                    statement.setBigDecimal(index++, dto.precio());
                    statement.setBoolean(index++, dto.cobroPorInvitado());
                }
            }
            if (actualizar) statement.setLong(index, dto.id());
            return statement.executeUpdate();
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ya existe un catálogo con ese nombre (no se distingue mayúsculas).", exception);
            }
            throw new DatabaseException("No se pudo guardar el registro del catálogo.", exception);
        }
    }
}
