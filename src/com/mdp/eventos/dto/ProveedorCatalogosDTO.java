package com.mdp.eventos.dto;

import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;

import java.util.List;

public record ProveedorCatalogosDTO(List<ZonaMdp> zonas, List<TipoEvento> tiposEvento) {
    public ProveedorCatalogosDTO {
        zonas = List.copyOf(zonas);
        tiposEvento = List.copyOf(tiposEvento);
    }
}
