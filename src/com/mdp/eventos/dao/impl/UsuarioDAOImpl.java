package com.mdp.eventos.dao.impl;

import com.mdp.eventos.config.DatabaseConnection;
import com.mdp.eventos.dao.IUsuarioDAO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAOImpl implements IUsuarioDAO {
    private static final String SELECT_USUARIO_AUTENTICACION = """
        SELECT u.id, u.rol_id, u.nombre_completo, u.email, u.password_hash, u.telefono,
               u.activo, r.nombre AS nombre_rol
        FROM usuarios u
        JOIN roles r ON r.id = u.rol_id
        """;
    private static final String SELECT_USUARIOS_ADMIN = """
        SELECT u.id, u.rol_id, u.nombre_completo, u.email, u.telefono, u.activo,
               r.nombre AS nombre_rol
        FROM usuarios u
        JOIN roles r ON r.id = u.rol_id
        """;

    @Override
    public Optional<Usuario> buscarPorEmail(String email) throws DatabaseException {
        String sql = SELECT_USUARIO_AUTENTICACION + " WHERE LOWER(u.email) = LOWER(?)";
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapear(resultSet, true)) : Optional.empty();
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DatabaseException("No se pudo consultar el usuario por email. " + detalle(exception), exception);
        }
    }

    @Override
    public List<Usuario> listar() throws DatabaseException {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SELECT_USUARIOS_ADMIN + " ORDER BY u.id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) usuarios.add(mapear(resultSet, false));
            return List.copyOf(usuarios);
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DatabaseException("No se pudieron listar los usuarios. " + detalle(exception), exception);
        }
    }

    @Override
    public boolean actualizarEstado(long usuarioId, boolean activo, long administradorId) throws DatabaseException {
        String selectTarget = """
            SELECT u.activo, r.nombre AS nombre_rol, r.id AS rol_id
            FROM usuarios u JOIN roles r ON r.id = u.rol_id
            WHERE u.id = ?
            FOR UPDATE
            """;
        String lockAdminRole = "SELECT id FROM roles WHERE nombre = ? FOR UPDATE";
        String countActiveAdmins = """
            SELECT COUNT(*) AS cantidad
            FROM usuarios u JOIN roles r ON r.id = u.rol_id
            WHERE r.nombre = ? AND u.activo = TRUE
            """;
        String update = "UPDATE usuarios SET activo = ? WHERE id = ? AND activo <> ?";
        try (Connection connection = DatabaseConnection.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                boolean currentActive;
                String role;
                try (PreparedStatement target = connection.prepareStatement(selectTarget)) {
                    target.setLong(1, usuarioId);
                    try (ResultSet row = target.executeQuery()) {
                        if (!row.next()) {
                            connection.rollback();
                            return false;
                        }

                        currentActive = row.getBoolean("activo");
                        role = row.getString("nombre_rol");
                    }
                }

                if (currentActive == activo) {
                    connection.commit();
                    return false;
                }
                if (Rol.ROLE_ADMIN.equals(role) && !activo) {
                    try (PreparedStatement lock = connection.prepareStatement(lockAdminRole)) {
                        lock.setString(1, Rol.ROLE_ADMIN);
                        lock.executeQuery().close();
                    }
                    try (PreparedStatement count = connection.prepareStatement(countActiveAdmins)) {
                        count.setString(1, Rol.ROLE_ADMIN);
                        try (ResultSet row = count.executeQuery()) {
                            if (row.next() && row.getLong("cantidad") <= 1) {
                                throw new DatabaseException(
                                    "No se puede desactivar al único administrador activo."
                                );
                            }
                        }
                    }
                }

                int updated;
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setBoolean(1, activo);
                    statement.setLong(2, usuarioId);
                    statement.setBoolean(3, activo);
                    updated = statement.executeUpdate();
                }
                if (updated == 1 && !activo && Rol.ROLE_PROVEEDOR.equals(role)) {
                    cancelarPendientesDeProveedor(connection, usuarioId, administradorId);
                    marcarConfirmadasDeProveedor(connection, usuarioId);
                }
                connection.commit();
                return updated == 1;
            } catch (SQLException | DatabaseException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("No se pudo cambiar el estado de la cuenta.", exception);
        }
    }

    private void cancelarPendientesDeProveedor(Connection connection, long proveedorId, long administradorId)
        throws SQLException {
        String sql = """
            UPDATE reservas r
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            SET r.estado = 'CANCELADA',
                r.motivo_cancelacion = 'Proveedor dado de baja',
                r.cancelada_por_usuario_id = ?,
                r.cancelada_en = CURRENT_TIMESTAMP
            WHERE e.proveedor_id = ? AND r.estado = 'PENDIENTE'
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, administradorId);
            statement.setLong(2, proveedorId);
            statement.executeUpdate();
        }
    }

    private void marcarConfirmadasDeProveedor(Connection connection, long proveedorId) throws SQLException {
        String sql = """
            UPDATE reservas r
            JOIN salones s ON s.id = r.salon_id
            JOIN establecimientos e ON e.id = s.establecimiento_id
            SET r.requiere_revision_admin = TRUE
            WHERE e.proveedor_id = ?
              AND r.estado = 'CONFIRMADA'
              AND TIMESTAMP(r.fecha_evento, r.hora_inicio) > CURRENT_TIMESTAMP
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, proveedorId);
            statement.executeUpdate();
        }
    }

    @Override
    public void registrar(String nombre, String email, String passwordHash, String rolNombre)
        throws DatabaseException {
        String sql = """
            INSERT INTO usuarios (rol_id, nombre_completo, email, password_hash, activo)
            SELECT id, ?, ?, ?, TRUE
            FROM roles
            WHERE nombre = ? AND nombre IN ('ROLE_CLIENTE', 'ROLE_PROVEEDOR')
            """;
        try (Connection connection = DatabaseConnection.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombre);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, rolNombre);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("El rol seleccionado no está disponible para registro.");
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new DatabaseException("Ese correo ya está registrado.", exception);
            }
            throw new DatabaseException("No se pudo registrar la cuenta.", exception);
        }
    }

    private Usuario mapear(ResultSet resultSet, boolean includePassword) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(resultSet.getLong("id"));
        usuario.setRolId(resultSet.getLong("rol_id"));
        usuario.setNombreCompleto(resultSet.getString("nombre_completo"));
        usuario.setEmail(resultSet.getString("email"));
        if (includePassword) usuario.setPasswordHash(resultSet.getString("password_hash"));
        usuario.setTelefono(resultSet.getString("telefono"));
        usuario.setActivo(resultSet.getBoolean("activo"));
        Rol rol = new Rol();
        rol.setId(usuario.getRolId());
        rol.setNombre(resultSet.getString("nombre_rol"));
        usuario.setRol(rol);
        return usuario;
    }

    private String detalle(Exception exception) {
        if (exception instanceof SQLException sqlException) {
            return "SQLState " + sqlException.getSQLState() + ": " + sqlException.getMessage();
        }
        return exception.getMessage();
    }
}
