package com.mdp.eventos.service;

import com.mdp.eventos.dao.ICatalogoAdminDAO;
import com.mdp.eventos.dto.CatalogoAdminDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.TipoCatalogoAdmin;

import java.util.List;
import java.util.Objects;

public final class CatalogoAdminService {
    private final ICatalogoAdminDAO catalogoDAO;

    public CatalogoAdminService(ICatalogoAdminDAO catalogoDAO) {
        this.catalogoDAO = Objects.requireNonNull(catalogoDAO, "catalogoDAO");
    }

    public List<CatalogoAdminDTO> listar(TipoCatalogoAdmin tipo)
        throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_ADMIN);
        validarTipo(tipo);
        return catalogoDAO.listar(tipo);
    }

    public void guardar(CatalogoAdminDTO dto) throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_ADMIN);
        validar(dto);
        if (dto.id() == 0) catalogoDAO.crear(dto);
        else if (!catalogoDAO.actualizar(dto)) {
            throw new BusinessValidationException("El registro de catálogo ya no existe.");
        }
    }

    public void cambiarEstado(TipoCatalogoAdmin tipo, long id, boolean activo)
        throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_ADMIN);
        validarTipo(tipo);
        if (id <= 0) throw new BusinessValidationException("Seleccione un registro válido.");
        if (!catalogoDAO.cambiarEstado(tipo, id, activo)) {
            throw new BusinessValidationException("El registro de catálogo ya no existe.");
        }
    }

    private void validar(CatalogoAdminDTO dto) throws BusinessValidationException {
        if (dto == null) throw new BusinessValidationException("Complete los datos del catálogo.");
        validarTipo(dto.tipo());
        String nombre = dto.nombre() == null ? "" : dto.nombre().trim();
        int limiteNombre = dto.tipo() == TipoCatalogoAdmin.SERVICIO_BASE ? 100 : 80;
        if (nombre.isEmpty() || nombre.length() > limiteNombre) {
            throw new BusinessValidationException(
                "El nombre es obligatorio y admite hasta " + limiteNombre + " caracteres."
            );
        }
        String descripcion = dto.descripcion() == null ? "" : dto.descripcion().trim();
        if (descripcion.length() > 200) {
            throw new BusinessValidationException("La descripción admite hasta 200 caracteres.");
        }
        String postal = dto.codigoPostal() == null ? "" : dto.codigoPostal().trim();
        if (dto.tipo() == TipoCatalogoAdmin.ZONA && (postal.isEmpty() || postal.length() > 10)) {
            throw new BusinessValidationException("El código postal es obligatorio y admite hasta 10 caracteres.");
        }
        if (dto.tipo() == TipoCatalogoAdmin.SERVICIO_BASE
            && (dto.precio() == null || dto.precio().signum() <= 0
                || dto.precio().scale() > 2 || dto.precio().precision() > 12)) {
            throw new BusinessValidationException("El precio sugerido debe ser mayor que cero y admitir 2 decimales.");
        }
    }

    private void validarTipo(TipoCatalogoAdmin tipo) throws BusinessValidationException {
        if (tipo == null) throw new BusinessValidationException("Seleccione un tipo de catálogo válido.");
    }
}
