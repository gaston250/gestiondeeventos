package com.mdp.eventos.controller;

import com.mdp.eventos.dto.CotizacionSalonDTO;
import com.mdp.eventos.dto.FiltroBusquedaEventoDTO;
import com.mdp.eventos.dto.CrearReservaDTO;
import com.mdp.eventos.exception.AppBaseException;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.service.EventoBusquedaService;
import com.mdp.eventos.service.ServicioAdicionalService;
import com.mdp.eventos.view.cliente.DetalleCotizacionDialog;
import com.mdp.eventos.view.cliente.BuscadorEventosView;
import com.mdp.eventos.view.components.ValidadorCamposSwing;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class ClienteController {
    private final BuscadorEventosView vista;
    private final EventoBusquedaService busquedaService;
    private final ReservaController reservaController;
    private final ServicioAdicionalService servicioAdicionalService;

    public ClienteController(
        BuscadorEventosView vista,
        EventoBusquedaService busquedaService,
        ReservaController reservaController
    ) {
        this(vista, busquedaService, reservaController,
            new ServicioAdicionalService(new com.mdp.eventos.dao.impl.ServicioAdicionalDAOImpl()));
    }

    public ClienteController(
        BuscadorEventosView vista,
        EventoBusquedaService busquedaService,
        ReservaController reservaController,
        ServicioAdicionalService servicioAdicionalService
    ) {
        this.vista = Objects.requireNonNull(vista, "vista");
        this.busquedaService = Objects.requireNonNull(busquedaService, "busquedaService");
        this.reservaController = Objects.requireNonNull(reservaController, "reservaController");
        this.servicioAdicionalService = Objects.requireNonNull(servicioAdicionalService, "servicioAdicionalService");
        vista.setController(this);
        cargarCatalogos();
    }

    private void cargarCatalogos() {
        vista.mostrarEstado("Cargando tipos de evento y zonas desde MySQL...");
        new SwingWorker<Catalogos, Void>() {
            @Override
            protected Catalogos doInBackground() throws Exception {
                return new Catalogos(busquedaService.listarTiposEvento(), busquedaService.listarZonas());
            }

            @Override
            protected void done() {
                try {
                    Catalogos catalogos = get();
                    vista.setCatalogos(catalogos.tipos(), catalogos.zonas());
                    vista.mostrarEstado("Seleccione criterios y busque salones disponibles.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarEstado("Se interrumpió la carga de catálogos de MySQL.");
                    vista.mostrarMensaje(
                        "Se interrumpió la carga de tipos de evento y zonas.",
                        "Carga cancelada",
                        JOptionPane.WARNING_MESSAGE
                    );
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    vista.mostrarEstado("No se pudieron cargar los catálogos de MySQL.");
                    vista.mostrarMensaje(
                        cause instanceof AppBaseException
                            ? cause.getMessage()
                            : "No se pudieron cargar tipos de evento y zonas desde MySQL.",
                        "Error de conexión",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    public void buscarDesdeVista() {
        LocalDate fecha;
        LocalTime horaInicio;
        LocalTime horaFin;
        try {
            fecha = ValidadorCamposSwing.parsearFecha(vista.getFechaIngresada());
            horaInicio = ValidadorCamposSwing.parsearHora(vista.getHoraInicioIngresada());
            horaFin = ValidadorCamposSwing.parsearHora(vista.getHoraFinIngresada());
        } catch (DateTimeParseException exception) {
            vista.mostrarMensaje(
                "Use AAAA-MM-DD para la fecha y HH:MM para las horas.",
                "Error de Formato",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        FiltroBusquedaEventoDTO filtro = new FiltroBusquedaEventoDTO(
            vista.getTipoEventoSeleccionado(),
            vista.getInvitadosSeleccionados(),
            fecha,
            horaInicio,
            horaFin,
            vista.getZonaSeleccionada()
        );
        SwingWorker<List<CotizacionSalonDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CotizacionSalonDTO> doInBackground() throws Exception {
                return busquedaService.buscarYCotizar(filtro);
            }

            @Override
            protected void done() {
                vista.setBusquedaActiva(false);
                try {
                    List<CotizacionSalonDTO> resultados = get();
                    vista.mostrarResultados(resultados);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarEstado("La búsqueda fue interrumpida.");
                    vista.mostrarMensaje(
                        "La búsqueda fue interrumpida.",
                        "Búsqueda cancelada",
                        JOptionPane.WARNING_MESSAGE
                    );
                } catch (ExecutionException exception) {
                    mostrarError(exception.getCause());
                }
            }
        };
        vista.setBusquedaActiva(true);
        vista.mostrarEstado("Consultando disponibilidad y clima en Mar del Plata...");
        worker.execute();
    }

    public void limpiar() {
        vista.limpiarControles();
    }

    public void confirmarReserva() {
        CotizacionSalonDTO seleccionada = vista.getCotizacionSeleccionada();
        if (seleccionada == null) {
            vista.mostrarMensaje(
                "Debe seleccionar un establecimiento de la tabla para continuar.",
                "Selección Requerida",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        LocalDate fecha;
        LocalTime horaInicio;
        LocalTime horaFin;
        try {
            fecha = ValidadorCamposSwing.parsearFecha(vista.getFechaIngresada());
            horaInicio = ValidadorCamposSwing.parsearHora(vista.getHoraInicioIngresada());
            horaFin = ValidadorCamposSwing.parsearHora(vista.getHoraFinIngresada());
        } catch (DateTimeParseException exception) {
            vista.mostrarMensaje("Revise la fecha y el horario de la reserva.", "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        vista.setBusquedaActiva(true);
        vista.mostrarEstado("Consultando servicios adicionales del establecimiento en MySQL...");
        new SwingWorker<List<ServicioAdicional>, Void>() {
            @Override
            protected List<ServicioAdicional> doInBackground() throws Exception {
                return servicioAdicionalService.listarPorSalon(seleccionada.salonId());
            }

            @Override
            protected void done() {
                vista.setBusquedaActiva(false);
                try {
                    List<ServicioAdicional> servicios = get();
                    DetalleCotizacionDialog dialog = new DetalleCotizacionDialog(
                        vista, seleccionada, fecha, vista.getTipoEventoNombre(), servicios
                    );
                    dialog.setVisible(true);
                    if (!dialog.isConfirmed()) return;

                    CrearReservaDTO solicitud = new CrearReservaDTO(
                        seleccionada.salonId(),
                        vista.getTipoEventoSeleccionado(),
                        fecha,
                        horaInicio,
                        horaFin,
                        vista.getInvitadosSeleccionados(),
                        dialog.getServiciosSeleccionadosIds(),
                        dialog.getTotalEstimado()
                    );
                    vista.setBusquedaActiva(true);
                    vista.mostrarEstado("Guardando la solicitud y servicios seleccionados en MySQL...");
                    reservaController.crear(
                        solicitud,
                        ClienteController.this::reservaCreada,
                        ClienteController.this::reservaFallida
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarEstado("Se interrumpió la consulta de servicios.");
                    vista.mostrarMensaje("Se interrumpió la consulta de servicios adicionales.",
                        "Consulta cancelada", JOptionPane.WARNING_MESSAGE);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    vista.mostrarEstado("No se pudieron cargar los servicios adicionales.");
                    mostrarError(cause);
                }
            }

        }.execute();
    }

    private void reservaCreada(Reserva reserva) {
        vista.setBusquedaActiva(false);
        vista.mostrarEstado("Reserva creada: " + reserva.getCodigoReserva());
        Object[] options = {"Ir a Mis reservas", "Seguir buscando"};
        int selected = JOptionPane.showOptionDialog(
            vista,
            "✓ Solicitud enviada al proveedor.\nLa reserva quedó pendiente de confirmación.\nCódigo: "
                + reserva.getCodigoReserva() + "\nTotal: "
                + java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-AR"))
                    .format(reserva.getMontoTotal()),
            "Reserva creada",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        if (selected == 0) vista.abrirMisReservas();
    }

    private void reservaFallida(Throwable error) {
        vista.setBusquedaActiva(false);
        if (error instanceof ScheduleConflictException) {
            vista.mostrarSinDisponibilidadReciente();
            vista.mostrarMensaje(
                error.getMessage() + "\n\nCambia la fecha o el horario en los filtros y vuelve a buscar.",
                "Horario ocupado",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        vista.mostrarEstado("No se pudo crear la reserva.");
        vista.mostrarMensaje(
            error.getMessage() == null ? "No se pudo crear la reserva." : error.getMessage(),
            "Error de Reserva",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void mostrarError(Throwable error) {
        vista.mostrarEstado("Error en la búsqueda.");
        if (error instanceof BusinessValidationException) {
            vista.mostrarMensaje(error.getMessage(), "Validación de Negocio", JOptionPane.WARNING_MESSAGE);
        } else if (error instanceof AppBaseException) {
            vista.mostrarMensaje(error.getMessage(), "Error del Sistema", JOptionPane.ERROR_MESSAGE);
        } else {
            vista.mostrarMensaje(
                error == null ? "Ocurrió un error inesperado." : error.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private record Catalogos(List<TipoEvento> tipos, List<ZonaMdp> zonas) {
    }
}
