package com.mdp.eventos.config;

import com.mdp.eventos.model.Usuario;

import java.util.Objects;
import java.util.Optional;

public final class SesionActual {
    private static volatile Usuario usuario;
    private static volatile Runnable alInvalidar = () -> { };

    private SesionActual() {
    }

    public static void iniciar(Usuario usuarioAutenticado) {
        usuario = Objects.requireNonNull(usuarioAutenticado, "usuarioAutenticado");
    }

    public static Optional<Usuario> getUsuario() {
        return Optional.ofNullable(usuario);
    }

    public static boolean tieneRol(String rolRequerido) {
        Objects.requireNonNull(rolRequerido, "rolRequerido");
        Usuario actual = usuario;
        return actual != null
            && actual.getRol() != null
            && rolRequerido.equals(actual.getRol().getNombre());
    }

    public static void cerrar() {
        usuario = null;
    }

    public static void configurarInvalidacion(Runnable accion) {
        alInvalidar = Objects.requireNonNull(accion, "accion");
    }

    public static void invalidar() {
        if (usuario == null) return;
        usuario = null;
        javax.swing.SwingUtilities.invokeLater(alInvalidar);
    }
}
