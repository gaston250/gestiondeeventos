package com.mdp.eventos.dto;

public record RegistroUsuarioDTO(
    String nombreCompleto,
    String email,
    String password,
    String telefono,
    String nombreRol
) {
}
