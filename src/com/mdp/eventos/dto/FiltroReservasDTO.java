package com.mdp.eventos.dto;

import java.time.LocalDate;

public record FiltroReservasDTO(String estado, LocalDate desde, LocalDate hasta) {
}
