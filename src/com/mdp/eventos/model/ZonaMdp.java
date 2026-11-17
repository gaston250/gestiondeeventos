package com.mdp.eventos.model;

public class ZonaMdp {
    private long id;
    private String nombre;
    private String codigoPostal;

    public ZonaMdp() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
}
