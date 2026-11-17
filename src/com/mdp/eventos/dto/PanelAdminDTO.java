package com.mdp.eventos.dto;

import com.mdp.eventos.model.Usuario;

import java.util.List;
import java.util.Objects;

public record PanelAdminDTO(
    ResumenAdminDTO resumen,
    List<Usuario> usuarios,
    List<ReservaAdminDTO> reservas,
    long totalReservas
) {
    public PanelAdminDTO {
        Objects.requireNonNull(resumen, "resumen");
        usuarios = List.copyOf(Objects.requireNonNull(usuarios, "usuarios"));
        reservas = List.copyOf(Objects.requireNonNull(reservas, "reservas"));
    }
}
