package com.mdp.eventos.service;

import com.mdp.eventos.dao.IEstablecimientoDAO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.model.Establecimiento;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;

import java.util.List;
import java.util.Objects;

public class EstablecimientoService {
    private final IEstablecimientoDAO establecimientoDAO;

    public EstablecimientoService(IEstablecimientoDAO establecimientoDAO) {
        this.establecimientoDAO = Objects.requireNonNull(establecimientoDAO, "establecimientoDAO");
    }

    public List<Establecimiento> listarDelProveedorActual()
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        return establecimientoDAO.listarPorProveedor(proveedor.getId());
    }

    public void crearDelProveedorActual(Establecimiento establecimiento)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        validarDatos(establecimiento);
        establecimiento.setProveedorId(proveedor.getId());
        establecimientoDAO.crearParaProveedor(establecimiento);
    }

    public void editarDelProveedorActual(Establecimiento establecimiento)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        if (establecimiento == null || establecimiento.getId() <= 0) {
            throw new BusinessValidationException("Seleccione un establecimiento válido.");
        }
        validarDatos(establecimiento);
        establecimiento.setProveedorId(proveedor.getId());
        if (!establecimientoDAO.actualizarParaProveedor(establecimiento, proveedor.getId())) {
            throw new BusinessValidationException(
                "No se pudo editar: el establecimiento no existe, no te pertenece o tiene reservas futuras confirmadas."
            );
        }
    }

    public void cambiarEstadoDelProveedorActual(long establecimientoId, boolean activo)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        if (establecimientoId <= 0) throw new BusinessValidationException("Seleccione un establecimiento válido.");
        if (!establecimientoDAO.cambiarEstadoParaProveedor(establecimientoId, proveedor.getId(), activo)) {
            throw new BusinessValidationException(
                "No se pudo cambiar el estado: verifica que el establecimiento te pertenezca y no tenga "
                    + "reservas futuras confirmadas."
            );
        }
    }

    private void validarDatos(Establecimiento establecimiento) throws BusinessValidationException {
        if (establecimiento == null) {
            throw new BusinessValidationException("Ingrese los datos del establecimiento.");
        }
        String nombre = limpiar(establecimiento.getNombreComercial());
        String direccion = limpiar(establecimiento.getDireccion());
        String cuitIngresado = limpiar(establecimiento.getCuit());
        if (!cuitIngresado.matches("[0-9 -]+")) {
            throw new BusinessValidationException("Ingrese un CUIT válido de 11 dígitos.");
        }
        String cuit = cuitIngresado.replace(" ", "").replace("-", "");
        if (nombre.isEmpty() || nombre.length() > 120) {
            throw new BusinessValidationException("El nombre comercial es obligatorio y admite hasta 120 caracteres.");
        }
        if (direccion.isEmpty() || direccion.length() > 200) {
            throw new BusinessValidationException("La dirección es obligatoria y admite hasta 200 caracteres.");
        }
        if (establecimiento.getZonaId() <= 0) {
            throw new BusinessValidationException("Seleccione una zona de Mar del Plata.");
        }
        if (!cuitValido(cuit)) {
            throw new BusinessValidationException("Ingrese un CUIT válido de 11 dígitos.");
        }
        establecimiento.setNombreComercial(nombre);
        establecimiento.setDireccion(direccion);
        establecimiento.setCuit(cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10));
    }

    private Usuario obtenerProveedorActual() throws BusinessValidationException, DatabaseException {
        return SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
    }

    private boolean cuitValido(String cuit) {
        if (!cuit.matches("\\d{11}")) return false;
        int[] factores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < factores.length; i++) {
            suma += Character.digit(cuit.charAt(i), 10) * factores[i];
        }
        int digito = 11 - suma % 11;
        if (digito == 11) digito = 0;
        if (digito == 10) digito = 9;
        return digito == Character.digit(cuit.charAt(10), 10);
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
