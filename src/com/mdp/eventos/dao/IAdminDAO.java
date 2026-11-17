package com.mdp.eventos.dao;

import com.mdp.eventos.dto.ReservaAdminDTO;
import com.mdp.eventos.dto.ResumenAdminDTO;
import com.mdp.eventos.exception.DatabaseException;

import java.util.List;

public interface IAdminDAO {
    ResumenAdminDTO obtenerResumen() throws DatabaseException;

    List<ReservaAdminDTO> listarReservas(int limite, int offset, String busqueda, String estado)
        throws DatabaseException;

    long contarReservas(String busqueda, String estado) throws DatabaseException;

    boolean cancelarReserva(long reservaId, long administradorId, String motivo) throws DatabaseException;
}
