package com.mdp.eventos.controller;

import com.mdp.eventos.dto.LoginRequestDTO;
import com.mdp.eventos.dto.RegistroUsuarioDTO;
import com.mdp.eventos.exception.AppBaseException;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.service.AuthService;
import com.mdp.eventos.view.auth.LoginView;

import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.function.Consumer;

public class AuthController {
    private final LoginView vista;
    private final AuthService authService;

    public AuthController(LoginView vista, AuthService authService, Consumer<Usuario> onLoginCorrecto) {
        this.vista = Objects.requireNonNull(vista, "vista");
        this.authService = Objects.requireNonNull(authService, "authService");
        Objects.requireNonNull(onLoginCorrecto, "onLoginCorrecto");
        vista.setController(this);
        vista.setOnLoginCorrecto(onLoginCorrecto);
    }

    public void ingresar() {
        char[] passwordChars = vista.getPasswordIngresada();
        String password = new String(passwordChars);
        java.util.Arrays.fill(passwordChars, '\0');
        LoginRequestDTO solicitud = new LoginRequestDTO(vista.getEmailIngresado(), password);
        vista.setAutenticando(true);
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                return authService.autenticar(solicitud);
            }

            @Override
            protected void done() {
                vista.setAutenticando(false);
                try {
                    Usuario usuario = get();
                    vista.limpiarPassword();
                    vista.loginCorrecto(usuario);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("La autenticación fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    vista.mostrarError(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "Ocurrió un error inesperado durante la autenticación.");
                }
            }
        }.execute();
    }

    public void registrar(RegistroUsuarioDTO solicitud) {
        if (solicitud == null) return;
        vista.setAutenticando(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                authService.registrar(solicitud);
                return null;
            }

            @Override
            protected void done() {
                vista.setAutenticando(false);
                try {
                    get();
                    vista.registroCorrecto(solicitud.email());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("El registro fue interrumpido.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    vista.mostrarError(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "No se pudo completar el registro.");
                }
            }
        }.execute();
    }
}
