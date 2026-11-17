package com.mdp.eventos.service;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.IAdminDAO;
import com.mdp.eventos.dto.PanelAdminDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Rol;

import java.util.Objects;

public final class AdminService {
    private final IAdminDAO adminDAO;
    private final UsuarioService usuarioService;

    public AdminService(IAdminDAO adminDAO, UsuarioService usuarioService) {
        this.adminDAO = Objects.requireNonNull(adminDAO, "adminDAO");
        this.usuarioService = Objects.requireNonNull(usuarioService, "usuarioService");
    }

    public PanelAdminDTO cargarPanel(int pagina, int tamanioPagina, String busqueda, String estado)
        throws BusinessValidationException, DatabaseException {
        exigirAdministrador();
        if (pagina < 0 || tamanioPagina <= 0 || tamanioPagina > 200) {
            throw new BusinessValidationException("La página solicitada no es válida.");
        }
        String texto = busqueda == null ? "" : busqueda.trim();
        if (texto.length() > 200) {
            throw new BusinessValidationException("El texto de búsqueda admite hasta 200 caracteres.");
        }
        String estadoNormalizado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(java.util.Locale.ROOT);
        if (estadoNormalizado != null) {
            try {
                com.mdp.eventos.model.EstadoReserva.valueOf(estadoNormalizado);
            } catch (IllegalArgumentException exception) {
                throw new BusinessValidationException("El estado de reserva seleccionado no es válido.", exception);
            }
        }
        int offset = Math.multiplyExact(pagina, tamanioPagina);
        return new PanelAdminDTO(
            adminDAO.obtenerResumen(),
            usuarioService.listarParaAdministracion(),
            adminDAO.listarReservas(tamanioPagina, offset, texto, estadoNormalizado),
            adminDAO.contarReservas(texto, estadoNormalizado)
        );
    }

    public void actualizarEstadoUsuario(long usuarioId, boolean activo)
        throws BusinessValidationException, DatabaseException {
        exigirAdministrador();
        usuarioService.actualizarEstado(usuarioId, activo);
    }

    public void cancelarReserva(long reservaId, String motivo)
        throws BusinessValidationException, DatabaseException {
        exigirAdministrador();
        String detalle = motivo == null ? "" : motivo.trim();
        if (reservaId <= 0 || detalle.isEmpty() || detalle.length() > 500) {
            throw new BusinessValidationException("El motivo es obligatorio y admite hasta 500 caracteres.");
        }
        long actorId = SesionActual.getUsuario()
            .orElseThrow(() -> new BusinessValidationException("La sesión de administrador no está activa."))
            .getId();
        if (!adminDAO.cancelarReserva(reservaId, actorId, detalle)) {
            throw new BusinessValidationException(
                "Solo pueden cancelarse reservas pendientes o confirmadas antes del inicio del evento."
            );
        }
    }

    private static void exigirAdministrador() throws BusinessValidationException, DatabaseException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_ADMIN);
    }
}
