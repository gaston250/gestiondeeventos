package com.mdp.eventos.model;

public class Usuario {
    private long id;
    private long rolId;
    private String nombreCompleto;
    private String email;
    private String passwordHash;
    private String telefono;
    private boolean activo;
    private Rol rol;

    public Usuario() {
    }

    public Usuario(long id, String nombreCompleto, String email, Rol rol) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.rol = rol;
        this.activo = true;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getRolId() { return rolId; }
    public void setRolId(long rolId) { this.rolId = rolId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
