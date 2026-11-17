package com.mdp.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SalonDisponibilidadDTO(
    long salonId,
    String establecimiento,
    String salon,
    String zona,
    String direccion,
    int capacidadMinima,
    int capacidadMaxima,
    BigDecimal tarifaBaseHora,
    BigDecimal costoPorPersona,
    LocalTime horaApertura,
    LocalTime horaCierre
) {
}
