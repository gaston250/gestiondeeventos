package com.mdp.eventos.dto;

import java.time.LocalDateTime;

public record ResumenAdminDTO(
    long usuariosRegistrados,
    long establecimientosActivos,
    long reservasPendientes,
    LocalDateTime ultimaReserva
) {
}
