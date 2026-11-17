package com.mdp.eventos.model;

public class Establecimiento {
    private long id;
    private long proveedorId;
    private long zonaId;
    private String cuit;
    private String nombreComercial;
    private String direccion;
    private boolean activo;

    public Establecimiento() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProveedorId() { return proveedorId; }
    public void setProveedorId(long proveedorId) { this.proveedorId = proveedorId; }
    public long getZonaId() { return zonaId; }
    public void setZonaId(long zonaId) { this.zonaId = zonaId; }
    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
