package com.mdp.eventos;

import com.mdp.eventos.controller.ClienteController;
import com.mdp.eventos.controller.AuthController;
import com.mdp.eventos.controller.ProveedorController;
import com.mdp.eventos.controller.ReservaController;
import com.mdp.eventos.dao.impl.EstablecimientoDAOImpl;
import com.mdp.eventos.dao.impl.AdminDAOImpl;
import com.mdp.eventos.dao.impl.EventoCatalogoDAOImpl;
import com.mdp.eventos.dao.impl.SalonDAOImpl;
import com.mdp.eventos.dao.impl.UsuarioDAOImpl;
import com.mdp.eventos.dao.impl.ReservaDAOImpl;
import com.mdp.eventos.dao.impl.ServicioAdicionalDAOImpl;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.service.AuthService;
import com.mdp.eventos.service.AdminService;
import com.mdp.eventos.service.EstablecimientoService;
import com.mdp.eventos.service.SalonService;
import com.mdp.eventos.service.ReservaService;
import com.mdp.eventos.service.EventoBusquedaService;
import com.mdp.eventos.service.UsuarioService;
import com.mdp.eventos.service.ServicioAdicionalService;
import com.mdp.eventos.view.admin.AdminDashboardFrame;
import com.mdp.eventos.controller.AdminController;
import com.mdp.eventos.view.auth.LoginView;
import com.mdp.eventos.view.cliente.BuscadorEventosView;
import com.mdp.eventos.view.cliente.MisReservasFrame;
import com.mdp.eventos.view.proveedor.GestionEstablecimientosView;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.SwingUtilities;
import java.awt.Window;

public final class MainApp {
    private MainApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ThemeMdp.applyGlobal();
            LoginView login = new LoginView();
            com.mdp.eventos.config.SesionActual.configurarInvalidacion(() -> {
                for (Window window : Window.getWindows()) {
                    if (window != login) window.dispose();
                }
                login.setVisible(true);
            });
            AuthService authService = new AuthService(new UsuarioDAOImpl());
            new AuthController(login, authService,
                usuario -> abrirVistaPorRol(usuario, login, authService));
            login.setVisible(true);
        });
    }

    private static void abrirVistaPorRol(Usuario usuario, LoginView login, AuthService authService) {
        Runnable cerrarSesion = () -> {
            authService.cerrarSesion();
            for (Window window : Window.getWindows()) {
                if (window != login) window.dispose();
            }
            login.setVisible(true);
        };
        if (Rol.ROLE_CLIENTE.equals(usuario.getRol().getNombre())) {
            BuscadorEventosView vista = new BuscadorEventosView(cerrarSesion);
            EventoBusquedaService servicio = new EventoBusquedaService(new SalonDAOImpl());
            ReservaService reservaService = new ReservaService(new ReservaDAOImpl(), new ServicioAdicionalDAOImpl());
            MisReservasFrame[] reservasRef = new MisReservasFrame[1];
            vista.setOnOpenReservations(() -> {
                if (reservasRef[0] == null) {
                    reservasRef[0] = new MisReservasFrame(
                        reservaService,
                        () -> {
                            reservasRef[0].setVisible(false);
                            vista.setVisible(true);
                        },
                        cerrarSesion
                    );
                }
                vista.setVisible(false);
                reservasRef[0].setVisible(true);
            });
            new ClienteController(
                vista,
                servicio,
                new ReservaController(reservaService),
                new ServicioAdicionalService(new ServicioAdicionalDAOImpl())
            );
            vista.setVisible(true);
        } else if (Rol.ROLE_ADMIN.equals(usuario.getRol().getNombre())) {
            AdminDashboardFrame vista = new AdminDashboardFrame(cerrarSesion);
            vista.setController(new AdminController(new AdminService(
                new AdminDAOImpl(),
                new UsuarioService(new UsuarioDAOImpl())
            )));
            vista.setVisible(true);
        } else {
            GestionEstablecimientosView vista = new GestionEstablecimientosView(cerrarSesion);
            vista.setController(new ProveedorController(
                new EstablecimientoService(new EstablecimientoDAOImpl()),
                new SalonService(new SalonDAOImpl()),
                new EventoCatalogoDAOImpl(),
                new ReservaService(new ReservaDAOImpl(), new ServicioAdicionalDAOImpl())
            ));
            vista.setVisible(true);
        }
    }
}
