package com.mdp.eventos.config;

import com.mdp.eventos.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection obtenerConexion() throws DatabaseException {
        AppConfig config = AppConfig.getInstance();
        try {
            return DriverManager.getConnection(
                config.getDatabaseUrl(),
                config.getDatabaseUser(),
                config.getDatabasePassword()
            );
        } catch (SQLException exception) {
            throw new DatabaseException(
                "No se pudo conectar con MySQL (SQLState " + exception.getSQLState() + "): "
                    + exception.getMessage(),
                exception
            );
        }
    }
}
