package com.mdp.eventos.model;

public class DisponibilidadHoraria {
    private long id;
    private long salonId;
    private String diaSemana;
    private boolean activo;

    public DisponibilidadHoraria() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSalonId() { return salonId; }
    public void setSalonId(long salonId) { this.salonId = salonId; }
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
