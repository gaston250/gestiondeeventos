package com.mdp.eventos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagoSena {
    private long id;
    private long reservaId;
    private BigDecimal monto;
    private String metodoPago;
    private LocalDateTime fechaPago;

    public PagoSena() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getReservaId() { return reservaId; }
    public void setReservaId(long reservaId) { this.reservaId = reservaId; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
