package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IEventoCatalogoDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventoCatalogoDAOImpl implements IEventoCatalogoDAO {
    @Override
    public List<TipoEvento> listarTiposEvento() throws DatabaseException {
        String sql = "SELECT id, nombre, descripcion FROM tipos_evento WHERE activo = TRUE ORDER BY nombre";
        List<TipoEvento> tipos = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                TipoEvento tipo = new TipoEvento();
                tipo.setId(resultSet.getLong("id"));
                tipo.setNombre(resultSet.getString("nombre"));
                tipo.setDescripcion(resultSet.getString("descripcion"));
                tipos.add(tipo);
            }
            return List.copyOf(tipos);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar los tipos de evento.", exception);
        }
    }

    @Override
    public List<ZonaMdp> listarZonas() throws DatabaseException {
        String sql = "SELECT id, nombre, codigo_postal FROM zonas_mdp WHERE activo = TRUE ORDER BY nombre";
        List<ZonaMdp> zonas = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ZonaMdp zona = new ZonaMdp();
                zona.setId(resultSet.getLong("id"));
                zona.setNombre(resultSet.getString("nombre"));
                zona.setCodigoPostal(resultSet.getString("codigo_postal"));
                zonas.add(zona);
            }
            return List.copyOf(zonas);
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudieron cargar las zonas de Mar del Plata.", exception);
        }
    }
}
