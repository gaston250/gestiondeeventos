package com.mdp.eventos.controller;

import com.mdp.eventos.dao.IEventoCatalogoDAO;
import com.mdp.eventos.dto.ProveedorCatalogosDTO;
import com.mdp.eventos.dto.ReservaProveedorDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.exception.AppBaseException;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.model.Establecimiento;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.service.ReservaService;
import com.mdp.eventos.service.SalonService;
import com.mdp.eventos.service.EstablecimientoService;
import com.mdp.eventos.service.ServicioAdicionalService;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ProveedorController {
    private final EstablecimientoService establecimientoService;
    private final SalonService salonService;
    private final IEventoCatalogoDAO catalogoDAO;
    private final ReservaService reservaService;
    private final ServicioAdicionalService servicioAdicionalService;

    public ProveedorController(
        EstablecimientoService establecimientoService,
        SalonService salonService,
        IEventoCatalogoDAO catalogoDAO,
        ReservaService reservaService
    ) {
        this(establecimientoService, salonService, catalogoDAO, reservaService,
            new ServicioAdicionalService(new com.mdp.eventos.dao.impl.ServicioAdicionalDAOImpl()));
    }

    public ProveedorController(
        EstablecimientoService establecimientoService,
        SalonService salonService,
        IEventoCatalogoDAO catalogoDAO,
        ReservaService reservaService,
        ServicioAdicionalService servicioAdicionalService
    ) {
        this.establecimientoService = Objects.requireNonNull(establecimientoService, "establecimientoService");
        this.salonService = Objects.requireNonNull(salonService, "salonService");
        this.catalogoDAO = Objects.requireNonNull(catalogoDAO, "catalogoDAO");
        this.reservaService = Objects.requireNonNull(reservaService, "reservaService");
        this.servicioAdicionalService = Objects.requireNonNull(servicioAdicionalService, "servicioAdicionalService");
    }

    public void cargarEstablecimientos(
        Consumer<List<Establecimiento>> alCompletar,
        Consumer<String> alFallar
    ) {
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        new SwingWorker<List<Establecimiento>, Void>() {
            @Override
            protected List<Establecimiento> doInBackground() throws Exception {
                return establecimientoService.listarDelProveedorActual();
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La consulta de establecimientos fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    alFallar.accept(cause instanceof AppBaseException
                        ? cause.getMessage()
                        : "No se pudieron cargar los establecimientos.");
                }
            }
        }.execute();
    }

    public void cargarCatalogos(
        Consumer<ProveedorCatalogosDTO> alCompletar,
        Consumer<String> alFallar
    ) {
        new SwingWorker<ProveedorCatalogosDTO, Void>() {
            @Override
            protected ProveedorCatalogosDTO doInBackground() throws Exception {
                com.mdp.eventos.service.SeguridadSesion.exigirUsuarioActivo(
                    com.mdp.eventos.model.Rol.ROLE_PROVEEDOR
                );
                return new ProveedorCatalogosDTO(
                    catalogoDAO.listarZonas(),
                    catalogoDAO.listarTiposEvento()
                );
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La carga de catálogos fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudieron cargar las zonas y tipos de evento."));
                }
            }
        }.execute();
    }

    public void crearEstablecimiento(Establecimiento establecimiento, Runnable alCompletar, Consumer<String> alFallar) {
        ejecutar(
            () -> establecimientoService.crearDelProveedorActual(establecimiento),
            alCompletar,
            alFallar,
            "No se pudo registrar el establecimiento."
        );
    }

    public void editarEstablecimiento(
        Establecimiento establecimiento, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(
            () -> establecimientoService.editarDelProveedorActual(establecimiento),
            alCompletar,
            alFallar,
            "No se pudo editar el establecimiento."
        );
    }

    public void cambiarEstadoEstablecimiento(
        long establecimientoId, boolean activo, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(
            () -> establecimientoService.cambiarEstadoDelProveedorActual(establecimientoId, activo),
            alCompletar,
            alFallar,
            "No se pudo cambiar el estado del establecimiento."
        );
    }

    public void crearSalon(
        Salon salon,
        List<Long> tiposEvento,
        List<String> diasDisponibles,
        Runnable alCompletar,
        Consumer<String> alFallar
    ) {
        ejecutar(
            () -> salonService.crearParaProveedorActual(salon, tiposEvento, diasDisponibles),
            alCompletar,
            alFallar,
            "No se pudo agregar el salón."
        );
    }

    public void cargarSalones(Consumer<List<SalonProveedorDTO>> alCompletar, Consumer<String> alFallar) {
        new SwingWorker<List<SalonProveedorDTO>, Void>() {
            @Override
            protected List<SalonProveedorDTO> doInBackground() throws Exception {
                return salonService.listarDelProveedorActual();
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La consulta de salones fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudieron cargar los salones."));
                }
            }
        }.execute();
    }

    public void editarSalon(
        Salon salon, List<Long> tipos, List<String> dias, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(() -> salonService.actualizarDelProveedorActual(salon, tipos, dias),
            alCompletar, alFallar, "No se pudo actualizar el salón.");
    }

    public void cambiarEstadoSalon(long salonId, boolean activo, Runnable alCompletar, Consumer<String> alFallar) {
        ejecutar(() -> salonService.cambiarEstadoDelProveedorActual(salonId, activo),
            alCompletar, alFallar, "No se pudo cambiar el estado del salón.");
    }

    public void cargarServicios(
        long salonId, Consumer<List<ServicioAdicional>> alCompletar, Consumer<String> alFallar
    ) {
        new SwingWorker<List<ServicioAdicional>, Void>() {
            @Override
            protected List<ServicioAdicional> doInBackground() throws Exception {
                return servicioAdicionalService.listarParaProveedor(salonId);
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La consulta de servicios fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudieron cargar los servicios."));
                }
            }
        }.execute();
    }

    public void crearServicio(
        ServicioAdicional servicio, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(() -> servicioAdicionalService.crearParaProveedor(servicio),
            alCompletar, alFallar, "No se pudo crear el servicio.");
    }

    public void editarServicio(
        ServicioAdicional servicio, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(() -> servicioAdicionalService.actualizarParaProveedor(servicio),
            alCompletar, alFallar, "No se pudo actualizar el servicio.");
    }

    public void cambiarEstadoServicio(
        long servicioId, long salonId, boolean activo, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(() -> servicioAdicionalService.cambiarEstadoParaProveedor(servicioId, salonId, activo),
            alCompletar, alFallar, "No se pudo cambiar el estado del servicio.");
    }

    public void cargarPlantillasServicio(
        java.util.function.Consumer<List<ServicioAdicional>> alCompletar, Consumer<String> alFallar
    ) {
        new SwingWorker<List<ServicioAdicional>, Void>() {
            @Override
            protected List<ServicioAdicional> doInBackground() throws Exception {
                return servicioAdicionalService.listarPlantillasParaProveedor();
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La carga de plantillas fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudieron cargar las plantillas."));
                }
            }
        }.execute();
    }

    public void agregarPlantillaServicio(
        long plantillaId, long salonId, Runnable alCompletar, Consumer<String> alFallar
    ) {
        ejecutar(() -> servicioAdicionalService.agregarPlantillaParaProveedor(plantillaId, salonId),
            alCompletar, alFallar, "No se pudo aplicar la plantilla.");
    }

    public void cargarReservasProveedor(
        Consumer<List<ReservaProveedorDTO>> alCompletar,
        Consumer<String> alFallar
    ) {
        new SwingWorker<List<ReservaProveedorDTO>, Void>() {
            @Override
            protected List<ReservaProveedorDTO> doInBackground() throws Exception {
                return reservaService.listarDelProveedorActual();
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept("La consulta de solicitudes fue interrumpida.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    alFallar.accept(mensajeError(exception.getCause(), "No se pudieron cargar las solicitudes."));
                }
            }
        }.execute();
    }

    public void actualizarEstadoReserva(
        long reservaId,
        EstadoReserva nuevoEstado,
        String motivo,
        Runnable alCompletar,
        Consumer<String> alFallar
    ) {
        ejecutar(
            () -> reservaService.actualizarEstadoComoProveedor(reservaId, nuevoEstado, motivo),
            alCompletar,
            alFallar,
            "No se pudo actualizar la solicitud."
        );
    }

    public void cancelarReserva(
        long reservaId,
        String motivo,
        Runnable alCompletar,
        Consumer<String> alFallar
    ) {
        ejecutar(
            () -> reservaService.cancelarComoProveedor(reservaId, motivo),
            alCompletar,
            alFallar,
            "No se pudo cancelar la reserva."
        );
    }

    private void ejecutar(
        Accion accion,
        Runnable alCompletar,
        Consumer<String> alFallar,
        String mensajeGenerico
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
                    alFallar.accept(mensajeError(exception.getCause(), mensajeGenerico));
                }
            }
        }.execute();
    }

    private String mensajeError(Throwable cause, String mensajeGenerico) {
        return cause instanceof AppBaseException && cause.getMessage() != null
            ? cause.getMessage()
            : mensajeGenerico;
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }
}
