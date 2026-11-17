package com.mdp.eventos.controller;

import com.mdp.eventos.dto.CrearReservaDTO;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.service.ReservaService;

import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.function.Consumer;

public class ReservaController {
    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = Objects.requireNonNull(reservaService, "reservaService");
    }

    public void crear(
        CrearReservaDTO solicitud,
        Consumer<Reserva> alCompletar,
        Consumer<Throwable> alFallar
    ) {
        Objects.requireNonNull(solicitud, "solicitud");
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");
        new SwingWorker<Reserva, Void>() {
            @Override
            protected Reserva doInBackground() throws Exception {
                return reservaService.crear(solicitud);
            }

            @Override
            protected void done() {
                try {
                    alCompletar.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    alFallar.accept(new IllegalStateException(
                        "La creación de la reserva fue interrumpida.", exception
                    ));
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    alFallar.accept(cause == null ? exception : cause);
                }
            }
        }.execute();
    }
}
