package com.mdp.eventos.dto;

import com.mdp.eventos.model.TipoCatalogoAdmin;

import java.math.BigDecimal;

public record CatalogoAdminDTO(
    long id,
    TipoCatalogoAdmin tipo,
    String nombre,
    String descripcion,
    String codigoPostal,
    BigDecimal precio,
    boolean cobroPorInvitado,
    boolean activo
) {
}
