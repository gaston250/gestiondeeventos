package com.mdp.eventos.service;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.IUsuarioDAO;
import com.mdp.eventos.dto.LoginRequestDTO;
import com.mdp.eventos.dto.RegistroUsuarioDTO;
import com.mdp.eventos.exception.AuthenticationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public class AuthService {
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int PASSWORD_KEY_BITS = 256;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final IUsuarioDAO usuarioDAO;

    public AuthService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO");
    }

    public Usuario autenticar(LoginRequestDTO solicitud) throws AuthenticationException, DatabaseException {
        if (solicitud == null || solicitud.email() == null || solicitud.email().isBlank()
            || solicitud.password() == null || solicitud.password().isBlank()) {
            throw new AuthenticationException("Ingrese email y contraseña.");
        }
        Usuario usuario = usuarioDAO.buscarPorEmail(solicitud.email().trim())
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new AuthenticationException("Credenciales inválidas."));
        if (!verificarPassword(solicitud.password(), usuario.getPasswordHash())) {
            throw new AuthenticationException("Credenciales inválidas.");
        }
        SesionActual.iniciar(usuario);
        return usuario;
    }

    public void registrar(RegistroUsuarioDTO solicitud)
        throws AuthenticationException, DatabaseException {
        if (solicitud == null) throw new AuthenticationException("Complete los datos de registro.");
        String nombre = solicitud.nombreCompleto() == null ? "" : solicitud.nombreCompleto().trim();
        String email = solicitud.email() == null ? "" : solicitud.email().trim().toLowerCase(Locale.ROOT);
        String password = solicitud.password();
        String role = solicitud.nombreRol();
        if (nombre.isEmpty() || nombre.length() > 120) {
            throw new AuthenticationException("El nombre es obligatorio y admite hasta 120 caracteres.");
        }
        if (email.length() > 120 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new AuthenticationException("Ingrese un correo electrónico válido.");
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new AuthenticationException("La contraseña debe tener entre 8 y 128 caracteres.");
        }
        if (!Rol.ROLE_CLIENTE.equals(role) && !Rol.ROLE_PROVEEDOR.equals(role)) {
            throw new AuthenticationException("Solo se permite registrar cuentas de cliente o proveedor.");
        }
        usuarioDAO.registrar(nombre, email, crearHashSeguro(password), role);
    }

    public void cerrarSesion() {
        SesionActual.cerrar();
    }

    private boolean verificarPassword(String password, String almacenado) throws AuthenticationException {
        if (almacenado != null && almacenado.startsWith("pbkdf2$")) {
            String[] parts = almacenado.split("\\$");
            if (parts.length != 4) return false;
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                return MessageDigest.isEqual(expected, derivar(password.toCharArray(), salt, iterations));
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        if (almacenado == null) return false;
        return MessageDigest.isEqual(
            hashLegado(password).getBytes(StandardCharsets.US_ASCII),
            almacenado.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String hashLegado(String password) throws AuthenticationException {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AuthenticationException("No está disponible SHA-256 para validar credenciales.", exception);
        }
    }

    private String crearHashSeguro(String password) throws AuthenticationException {
        byte[] salt = new byte[PASSWORD_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] result = derivar(password.toCharArray(), salt, PASSWORD_ITERATIONS);
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
            + "$" + Base64.getEncoder().encodeToString(result);
    }

    private byte[] derivar(char[] password, byte[] salt, int iterations) throws AuthenticationException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, PASSWORD_KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException exception) {
            throw new AuthenticationException("No se pudo proteger la contraseña.", exception);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
}
