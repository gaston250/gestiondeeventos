package com.mdp.eventos.controller;

import com.mdp.eventos.dto.PanelAdminDTO;
import com.mdp.eventos.dto.CatalogoAdminDTO;
import com.mdp.eventos.exception.AppBaseException;
import com.mdp.eventos.service.AdminService;
import com.mdp.eventos.service.CatalogoAdminService;
import com.mdp.eventos.model.TipoCatalogoAdmin;

import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.function.Consumer;

public final class AdminController {
    private final AdminService adminService;
    private final CatalogoAdminService catalogoService;

    public AdminController(AdminService adminService) {
        this(adminService, new CatalogoAdminService(new com.mdp.eventos.dao.impl.CatalogoAdminDAOImpl()));
    }

    public AdminController(AdminService adminService, CatalogoAdminService catalogoService) {
        this.adminService = Objects.requireNonNull(adminService, "adminService");
        this.catalogoService = Objects.requireNonNull(catalogoService, "catalogoService");
    }

    public void cargarCatalogo(
        TipoCatalogoAdmin tipo, Consumer<java.util.List<CatalogoAdminDTO>> alCompletar, Consumer<String> alFallar
    ) {
        new SwingWorker<java.util.List<CatalogoAdminDTO>, Void>() {
            @Override
            protected java.util.List<CatalogoAdminDTO> doInBackground() throws Exception {
                return catalogoService.listar(tipo);
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La consulta del catálogo fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudo cargar el catálogo."));
                }
            }
        }.execute();
    }

    public void guardarCatalogo(CatalogoAdminDTO dto, Runnable alCompletar, Consumer<String> alFallar) {
        ejecutarCatalogo(() -> catalogoService.guardar(dto), alCompletar, alFallar, "No se pudo guardar el catálogo.");
    }

    public void cambiarEstadoCatalogo(
        TipoCatalogoAdmin tipo, long id, boolean activo, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutarCatalogo(() -> catalogoService.cambiarEstado(tipo, id, activo), alCompletar, alFallar,
            "No se pudo cambiar el estado del catálogo.");
    }

    private void ejecutarCatalogo(
        AccionCatalogo accion, Runnable alCompletar, Consumer<String> alFallar, String mensaje
    ) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                accion.ejecutar();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alCompletar.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La operación fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), mensaje));
                }
            }
        }.execute();
    }

    private String mensajeError(Throwable cause, String fallback) {
        return cause instanceof AppBaseException && cause.getMessage() != null ? cause.getMessage() : fallback;
    }

    @FunctionalInterface
    private interface AccionCatalogo {
        void ejecutar() throws Exception;
    }

    public void cargar(
        int pagina, int tamanioPagina, String busqueda, String estado,
        Consumer<PanelAdminDTO> alCompletar, Consumer<String> alFallar
    ) {
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        new SwingWorker<PanelAdminDTO, Void>() {
            @Override
            protected PanelAdminDTO doInBackground() throws Exception {
                return adminService.cargarPanel(pagina, tamanioPagina, busqueda, estado);
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La carga del panel administrativo fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    alFallar.accept(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "No se pudo cargar el panel administrativo.");
                }
            }
        }.execute();
    }

    public void cancelarReserva(long reservaId, String motivo, Runnable alCompletar, Consumer<String> alFallar) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                adminService.cancelarReserva(reservaId, motivo);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alCompletar.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La cancelación fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    alFallar.accept(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "No se pudo cancelar la reserva.");
                }
            }
        }.execute();
    }

    public void actualizarEstadoUsuario(
        long usuarioId,
        boolean activo,
        Runnable alCompletar,
        Consumer<String> alFallar
    ) {
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                adminService.actualizarEstadoUsuario(usuarioId, activo);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alCompletar.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("El cambio de estado fue interrumpido.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    alFallar.accept(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "No se pudo cambiar el estado de la cuenta.");
                }
            }
        }.execute();
    }
}
