package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Establecimiento;

import java.util.List;

public interface IEstablecimientoDAO {
    List<Establecimiento> listarPorProveedor(long proveedorId) throws DatabaseException;

    void crearParaProveedor(Establecimiento establecimiento) throws DatabaseException;

    boolean actualizarParaProveedor(Establecimiento establecimiento, long proveedorId) throws DatabaseException;

    boolean cambiarEstadoParaProveedor(long establecimientoId, long proveedorId, boolean activo)
        throws DatabaseException;
}
