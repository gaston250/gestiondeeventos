package com.mdp.eventos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record FiltroBusquedaEventoDTO(
    int tipoEventoId,
    int invitados,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String zona
) {
}
