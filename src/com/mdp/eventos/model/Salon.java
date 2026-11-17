package com.mdp.eventos.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public class Salon {
    private long id;
    private long establecimientoId;
    private String nombreSalon;
    private int capacidadMinima;
    private int capacidadMaxima;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private BigDecimal tarifaBaseHora;
    private BigDecimal costoPorPersona;
    private boolean activo = true;

    public Salon() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getEstablecimientoId() { return establecimientoId; }
    public void setEstablecimientoId(long establecimientoId) { this.establecimientoId = establecimientoId; }
    public String getNombreSalon() { return nombreSalon; }
    public void setNombreSalon(String nombreSalon) { this.nombreSalon = nombreSalon; }
    public int getCapacidadMinima() { return capacidadMinima; }
    public void setCapacidadMinima(int capacidadMinima) { this.capacidadMinima = capacidadMinima; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public void setCapacidadMaxima(int capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }
    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
    public BigDecimal getTarifaBaseHora() { return tarifaBaseHora; }
    public void setTarifaBaseHora(BigDecimal tarifaBaseHora) { this.tarifaBaseHora = tarifaBaseHora; }
    public BigDecimal getCostoPorPersona() { return costoPorPersona; }
    public void setCostoPorPersona(BigDecimal costoPorPersona) { this.costoPorPersona = costoPorPersona; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
