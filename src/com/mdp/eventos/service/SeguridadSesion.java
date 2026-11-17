package com.mdp.eventos.service;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.impl.UsuarioDAOImpl;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Usuario;

public final class SeguridadSesion {
    private SeguridadSesion() {
    }

    public static Usuario exigirUsuarioActivo(String rolRequerido)
        throws BusinessValidationException, DatabaseException {
        Usuario sesion = SesionActual.getUsuario()
            .orElseThrow(() -> new BusinessValidationException("La sesión no está activa."));
        Usuario vigente = new UsuarioDAOImpl().buscarPorEmail(sesion.getEmail())
            .filter(usuario -> usuario.getId() == sesion.getId() && usuario.isActivo())
            .orElseThrow(() -> {
                SesionActual.invalidar();
                return new BusinessValidationException("La cuenta está inactiva o la sesión expiró.");
            });
        if (vigente.getRol() == null || !rolRequerido.equals(vigente.getRol().getNombre())) {
            throw new BusinessValidationException("La cuenta no tiene permisos para esta operación.");
        }
        return vigente;
    }
}
