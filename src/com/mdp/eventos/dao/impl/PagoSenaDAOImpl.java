package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IPagoSenaDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.PagoSena;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PagoSenaDAOImpl implements IPagoSenaDAO {
    @Override
    public PagoSena registrar(PagoSena pago) throws DatabaseException {
        String sql = "INSERT INTO pagos_sena (reserva_id, monto, metodo_pago) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, pago.getReservaId());
            statement.setBigDecimal(2, pago.getMonto());
            statement.setString(3, pago.getMetodoPago());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id del pago.");
                pago.setId(keys.getLong(1));
            }
            return pago;
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo registrar el pago de seña.", exception);
        }
    }
}
