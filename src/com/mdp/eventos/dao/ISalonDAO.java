package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.model.Salon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ISalonDAO {
    List<SalonDisponibilidadDTO> buscarDisponibles(
        int tipoEventoId,
        int invitados,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String zona
    ) throws DatabaseException;

    void crearParaProveedor(
        Salon salon,
        long proveedorId,
        List<Long> tiposEvento,
        List<String> diasDisponibles
    ) throws DatabaseException;

    List<SalonProveedorDTO> listarPorProveedor(long proveedorId) throws DatabaseException;

    List<String> actualizarParaProveedor(
        Salon salon, long proveedorId, List<Long> tiposEvento, List<String> diasDisponibles
    ) throws DatabaseException;

    List<String> cambiarEstadoParaProveedor(long salonId, long proveedorId, boolean activo)
        throws DatabaseException;

}
