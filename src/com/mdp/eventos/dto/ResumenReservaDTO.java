package com.mdp.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ResumenReservaDTO(
    long reservaId,
    String codigoReserva,
    String establecimiento,
    String salon,
    LocalDate fechaEvento,
    LocalTime horaInicio,
    LocalTime horaFin,
    int cantidadInvitados,
    BigDecimal montoTotal,
    String estado,
    String motivoEstado
) {
}
