package com.mdp.eventos.model;

import java.math.BigDecimal;

public class ServicioAdicional {
    private long id;
    private long establecimientoId;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private boolean cobroPorInvitado;
    private long salonId;
    private boolean activo = true;

    public ServicioAdicional() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getEstablecimientoId() { return establecimientoId; }
    public void setEstablecimientoId(long establecimientoId) { this.establecimientoId = establecimientoId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public boolean isCobroPorInvitado() { return cobroPorInvitado; }
    public void setCobroPorInvitado(boolean cobroPorInvitado) { this.cobroPorInvitado = cobroPorInvitado; }
    public long getSalonId() { return salonId; }
    public void setSalonId(long salonId) { this.salonId = salonId; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
