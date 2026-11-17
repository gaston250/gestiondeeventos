package com.mdp.eventos.model;

public class Rol {
    public static final String ROLE_CLIENTE = "ROLE_CLIENTE";
    public static final String ROLE_PROVEEDOR = "ROLE_PROVEEDOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private long id;
    private String nombre;
    private String descripcion;

    public Rol() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
