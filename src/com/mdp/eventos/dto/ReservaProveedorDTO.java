package com.mdp.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaProveedorDTO(
    long reservaId,
    String codigo,
    String cliente,
    String emailCliente,
    String establecimiento,
    String salon,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    int invitados,
    BigDecimal total,
    String estado,
    String motivoEstado
) {
}
