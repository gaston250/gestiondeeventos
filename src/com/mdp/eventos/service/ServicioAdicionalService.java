package com.mdp.eventos.service;

import com.mdp.eventos.dao.IServicioAdicionalDAO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.ServicioAdicional;

import java.util.List;
import java.util.Objects;

public class ServicioAdicionalService {
    private final IServicioAdicionalDAO servicioDAO;

    public ServicioAdicionalService(IServicioAdicionalDAO servicioDAO) {
        this.servicioDAO = Objects.requireNonNull(servicioDAO, "servicioDAO");
    }

    public List<ServicioAdicional> listarPorSalon(long salonId)
        throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_CLIENTE);
        if (salonId <= 0) {
            throw new BusinessValidationException("Seleccione un salón válido para consultar sus servicios.");
        }
        return servicioDAO.listarPorSalon(salonId);
    }

    public List<ServicioAdicional> listarParaProveedor(long salonId)
        throws DatabaseException, BusinessValidationException {
        com.mdp.eventos.model.Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        if (salonId <= 0) throw new BusinessValidationException("Seleccione un salón válido.");
        return servicioDAO.listarPorSalonProveedor(salonId, proveedor.getId());
    }

    public void crearParaProveedor(ServicioAdicional servicio)
        throws DatabaseException, BusinessValidationException {
        com.mdp.eventos.model.Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        validar(servicio);
        servicioDAO.crearParaSalon(servicio, proveedor.getId());
    }

    public void actualizarParaProveedor(ServicioAdicional servicio)
        throws DatabaseException, BusinessValidationException {
        com.mdp.eventos.model.Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        if (servicio == null || servicio.getId() <= 0 || servicio.getSalonId() <= 0) {
            throw new BusinessValidationException("Seleccione un servicio válido.");
        }
        validar(servicio);
        if (!servicioDAO.actualizarParaSalon(servicio, proveedor.getId())) {
            throw new BusinessValidationException("El servicio no existe o no pertenece a un salón propio.");
        }
    }

    public void cambiarEstadoParaProveedor(long servicioId, long salonId, boolean activo)
        throws DatabaseException, BusinessValidationException {
        com.mdp.eventos.model.Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        if (servicioId <= 0 || salonId <= 0) throw new BusinessValidationException("Seleccione un servicio válido.");
        if (!servicioDAO.cambiarEstadoParaSalon(servicioId, salonId, proveedor.getId(), activo)) {
            throw new BusinessValidationException("El servicio no existe o no pertenece a un salón propio.");
        }
    }

    public List<ServicioAdicional> listarPlantillasParaProveedor()
        throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        return servicioDAO.listarPlantillasActivas();
    }

    public void agregarPlantillaParaProveedor(long plantillaId, long salonId)
        throws DatabaseException, BusinessValidationException {
        com.mdp.eventos.model.Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        if (plantillaId <= 0 || salonId <= 0) {
            throw new BusinessValidationException("Seleccione una plantilla y un salón válidos.");
        }
        servicioDAO.crearDesdePlantilla(plantillaId, salonId, proveedor.getId());
    }

    private void validar(ServicioAdicional servicio) throws BusinessValidationException {
        if (servicio == null || servicio.getSalonId() <= 0) {
            throw new BusinessValidationException("Seleccione un salón propio.");
        }
        String nombre = servicio.getNombre() == null ? "" : servicio.getNombre().trim();
        String descripcion = servicio.getDescripcion() == null ? "" : servicio.getDescripcion().trim();
        if (nombre.isEmpty() || nombre.length() > 100) {
            throw new BusinessValidationException("El nombre es obligatorio y admite hasta 100 caracteres.");
        }
        if (descripcion.length() > 200) {
            throw new BusinessValidationException("La descripción admite hasta 200 caracteres.");
        }
        if (servicio.getPrecio() == null || servicio.getPrecio().signum() <= 0
            || servicio.getPrecio().scale() > 2 || servicio.getPrecio().precision() > 12) {
            throw new BusinessValidationException("El precio debe ser mayor que cero y admitir hasta 2 decimales.");
        }
        servicio.setNombre(nombre);
        servicio.setDescripcion(descripcion.isBlank() ? null : descripcion);
    }
}
