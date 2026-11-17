package com.mdp.eventos.dto;

import com.mdp.eventos.model.Salon;

import java.util.List;

public record SalonProveedorDTO(Salon salon, String establecimiento, List<Long> tiposEventoIds, List<String> dias) {
    public SalonProveedorDTO {
        tiposEventoIds = List.copyOf(tiposEventoIds);
        dias = List.copyOf(dias);
    }
}
