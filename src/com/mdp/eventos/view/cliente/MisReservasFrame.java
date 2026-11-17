package com.mdp.eventos.view.cliente;

import com.mdp.eventos.controller.ClienteReservasController;
import com.mdp.eventos.service.ReservaService;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

public final class MisReservasFrame extends JFrame {
    public MisReservasFrame(
        ReservaService reservaService,
        Runnable onBack,
        Runnable onLogout
    ) {
        super("Eventos MDP | Mis reservas");
        MisReservasPanel panel = new MisReservasPanel(
            Objects.requireNonNull(onBack, "onBack"),
            Objects.requireNonNull(onLogout, "onLogout")
        );
        setContentPane(panel);
        new ClienteReservasController(panel, Objects.requireNonNull(reservaService, "reservaService"));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onBack.run();
            }
        });
        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }
}
