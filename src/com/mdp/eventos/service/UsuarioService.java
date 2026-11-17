package com.mdp.eventos.service;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.IUsuarioDAO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;

import java.util.Objects;
import java.util.List;

public class UsuarioService {
    private final IUsuarioDAO usuarioDAO;

    public UsuarioService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO");
    }

    public List<Usuario> listarParaAdministracion() throws BusinessValidationException, DatabaseException {
        exigirAdministrador();
        return usuarioDAO.listar();
    }

    public void actualizarEstado(long usuarioId, boolean activo)
        throws BusinessValidationException, DatabaseException {
        exigirAdministrador();
        Usuario actual = SesionActual.getUsuario()
            .orElseThrow(() -> new BusinessValidationException("La sesión de administrador no está activa."));
        if (actual.getId() == usuarioId) {
            throw new BusinessValidationException("No puedes desactivar tu propia cuenta mientras estás conectado.");
        }
        if (!usuarioDAO.actualizarEstado(usuarioId, activo, actual.getId())) {
            throw new BusinessValidationException("La cuenta ya tiene ese estado o no existe.");
        }
    }

    private static void exigirAdministrador() throws BusinessValidationException, DatabaseException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_ADMIN);
    }
}
