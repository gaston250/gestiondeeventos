package com.mdp.eventos.model;

import java.math.BigDecimal;

public class DetalleReservaServicio {
    private long reservaId;
    private long servicioId;
    private int cantidad;
    private BigDecimal precioUnitarioPactado;

    public DetalleReservaServicio() {
    }

    public long getReservaId() { return reservaId; }
    public void setReservaId(long reservaId) { this.reservaId = reservaId; }
    public long getServicioId() { return servicioId; }
    public void setServicioId(long servicioId) { this.servicioId = servicioId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitarioPactado() { return precioUnitarioPactado; }
    public void setPrecioUnitarioPactado(BigDecimal precioUnitarioPactado) { this.precioUnitarioPactado = precioUnitarioPactado; }
}
