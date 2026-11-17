package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.dto.ResumenReservaDTO;
import com.mdp.eventos.dto.ReservaProveedorDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IReservaDAO {
    CondicionesSalon buscarCondicionesSalon(
        long salonId,
        int tipoEventoId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin
    )
        throws DatabaseException;

    Reserva crear(Reserva reserva) throws DatabaseException, ScheduleConflictException;

    List<ResumenReservaDTO> listarPorCliente(
        long clienteId,
        EstadoReserva estado,
        LocalDate desde,
        LocalDate hasta
    ) throws DatabaseException;

    boolean cancelarAntesDelInicio(long reservaId, long clienteId) throws DatabaseException;

    List<ReservaProveedorDTO> listarPorProveedor(long proveedorId) throws DatabaseException;

    boolean actualizarEstadoPendiente(
        long reservaId,
        long proveedorId,
        EstadoReserva nuevoEstado,
        String motivo
    ) throws DatabaseException;

    boolean cancelarPorProveedor(long reservaId, long proveedorId, long actorId, String motivo)
        throws DatabaseException;

    boolean cancelarPorAdministrador(long reservaId, long actorId, String motivo) throws DatabaseException;

    void actualizarEstadosAutomaticos() throws DatabaseException;

    record CondicionesSalon(
        int capacidadMinima,
        int capacidadMaxima,
        LocalTime horaApertura,
        LocalTime horaCierre,
        BigDecimal tarifaBaseHora,
        BigDecimal costoPorPersona
    ) {
    }
}
