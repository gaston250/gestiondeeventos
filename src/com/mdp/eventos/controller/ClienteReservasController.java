package com.mdp.eventos.controller;

import com.mdp.eventos.dto.FiltroReservasDTO;
import com.mdp.eventos.dto.ResumenReservaDTO;
import com.mdp.eventos.exception.AppBaseException;
import com.mdp.eventos.service.ReservaService;
import com.mdp.eventos.view.cliente.MisReservasPanel;
import com.mdp.eventos.view.components.ValidadorCamposSwing;

import javax.swing.SwingWorker;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class ClienteReservasController {
    private final MisReservasPanel vista;
    private final ReservaService reservaService;

    public ClienteReservasController(MisReservasPanel vista, ReservaService reservaService) {
        this.vista = Objects.requireNonNull(vista, "vista");
        this.reservaService = Objects.requireNonNull(reservaService, "reservaService");
        vista.setController(this);
        cargar();
    }

    public void cargar() {
        cargar(null);
    }

    private void cargar(String resultadoCancelacion) {
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = parsearOpcional(vista.getDesdeIngresado());
            hasta = parsearOpcional(vista.getHastaIngresado());
        } catch (DateTimeParseException exception) {
            vista.mostrarError("Use el formato AAAA-MM-DD en las fechas.");
            return;
        }
        String estado = vista.getEstadoSeleccionado();
        vista.setCargando(true);
        new SwingWorker<List<ResumenReservaDTO>, Void>() {
            @Override
            protected List<ResumenReservaDTO> doInBackground() throws Exception {
                return reservaService.listarDelCliente(new FiltroReservasDTO(estado, desde, hasta));
            }

            @Override
            protected void done() {
                vista.setCargando(false);
                try {
                    List<ResumenReservaDTO> reservas = get();
                    vista.mostrarReservas(reservas);
                    if (resultadoCancelacion != null) {
                        vista.mostrarEstado(resultadoCancelacion);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("La consulta de reservas fue interrumpida.");
                } catch (ExecutionException exception) {
                    vista.mostrarError(mensaje(exception.getCause()));
                }
            }
        }.execute();
    }

    public void cancelar(long reservaId) {
        vista.setCargando(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return reservaService.cancelarDelCliente(reservaId);
            }

            @Override
            protected void done() {
                vista.setCargando(false);
                try {
                    boolean cancelada = get();
                    cargar(cancelada
                        ? "Reserva cancelada. Se muestran las reservas actualizadas."
                        : "No se canceló: el evento ya comenzó, la reserva ya fue cancelada o no pertenece a su cuenta.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("La cancelación fue interrumpida.");
                } catch (ExecutionException exception) {
                    vista.mostrarError(mensaje(exception.getCause()));
                }
            }
        }.execute();
    }

    private static LocalDate parsearOpcional(String value) {
        return value == null || value.isBlank()
            ? null
            : ValidadorCamposSwing.parsearFecha(value.trim());
    }

    private static String mensaje(Throwable error) {
        if (error instanceof AppBaseException) return error.getMessage();
        return error == null || error.getMessage() == null
            ? "No se pudo completar la operación de reservas."
            : error.getMessage();
    }
}
