package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.ServicioAdicional;

import java.util.List;

public interface IServicioAdicionalDAO {
    List<ServicioAdicional> listarPorSalon(long salonId) throws DatabaseException;

    List<ServicioAdicional> listarPorSalonProveedor(long salonId, long proveedorId) throws DatabaseException;

    void crearParaSalon(ServicioAdicional servicio, long proveedorId) throws DatabaseException;

    boolean actualizarParaSalon(ServicioAdicional servicio, long proveedorId) throws DatabaseException;

    boolean cambiarEstadoParaSalon(long servicioId, long salonId, long proveedorId, boolean activo)
        throws DatabaseException;

    List<ServicioAdicional> listarPlantillasActivas() throws DatabaseException;

    void crearDesdePlantilla(long plantillaId, long salonId, long proveedorId) throws DatabaseException;
}
