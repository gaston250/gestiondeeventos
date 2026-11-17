package com.mdp.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CrearReservaDTO(
    long salonId,
    int tipoEventoId,
    LocalDate fechaEvento,
    LocalTime horaInicio,
    LocalTime horaFin,
    int cantidadInvitados,
    List<Long> serviciosAdicionalesIds,
    BigDecimal montoTotalEsperado
) {
    public CrearReservaDTO(
        long salonId,
        int tipoEventoId,
        LocalDate fechaEvento,
        LocalTime horaInicio,
        LocalTime horaFin,
        int cantidadInvitados
    ) {
        this(salonId, tipoEventoId, fechaEvento, horaInicio, horaFin, cantidadInvitados, List.of(), null);
    }

    public CrearReservaDTO(
        long salonId,
        int tipoEventoId,
        LocalDate fechaEvento,
        LocalTime horaInicio,
        LocalTime horaFin,
        int cantidadInvitados,
        List<Long> serviciosAdicionalesIds
    ) {
        this(salonId, tipoEventoId, fechaEvento, horaInicio, horaFin, cantidadInvitados, serviciosAdicionalesIds, null);
    }

    public CrearReservaDTO {
        serviciosAdicionalesIds = List.copyOf(serviciosAdicionalesIds);
        if (serviciosAdicionalesIds.stream().distinct().count() != serviciosAdicionalesIds.size()) {
            throw new IllegalArgumentException("No se puede incluir dos veces el mismo servicio adicional.");
        }
    }
}
