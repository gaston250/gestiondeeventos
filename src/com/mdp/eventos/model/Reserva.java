package com.mdp.eventos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class Reserva {
    private long id;
    private String codigoReserva;
    private long clienteId;
    private long salonId;
    private long tipoEventoId;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int cantidadInvitados;
    private BigDecimal montoTotal;
    private EstadoReserva estado;
    private List<DetalleReservaServicio> serviciosAdicionales = List.of();

    public Reserva() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }
    public long getClienteId() { return clienteId; }
    public void setClienteId(long clienteId) { this.clienteId = clienteId; }
    public long getSalonId() { return salonId; }
    public void setSalonId(long salonId) { this.salonId = salonId; }
    public long getTipoEventoId() { return tipoEventoId; }
    public void setTipoEventoId(long tipoEventoId) { this.tipoEventoId = tipoEventoId; }
    public LocalDate getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDate fechaEvento) { this.fechaEvento = fechaEvento; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public int getCantidadInvitados() { return cantidadInvitados; }
    public void setCantidadInvitados(int cantidadInvitados) { this.cantidadInvitados = cantidadInvitados; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public List<DetalleReservaServicio> getServiciosAdicionales() { return serviciosAdicionales; }
    public void setServiciosAdicionales(List<DetalleReservaServicio> serviciosAdicionales) {
        this.serviciosAdicionales = List.copyOf(Objects.requireNonNull(serviciosAdicionales, "serviciosAdicionales"));
    }
}
