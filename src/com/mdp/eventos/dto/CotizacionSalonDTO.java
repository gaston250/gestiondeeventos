package com.mdp.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CotizacionSalonDTO(
    long salonId,
    String establecimiento,
    String salon,
    String zona,
    String direccion,
    int capacidadMaxima,
    int invitados,
    LocalTime horaInicio,
    LocalTime horaFin,
    BigDecimal precioTotalEstimado,
    BigDecimal tarifaBaseHora,
    BigDecimal costoPorPersona
) {
}
