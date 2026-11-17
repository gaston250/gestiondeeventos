package com.mdp.eventos.view.components;

import com.mdp.eventos.model.Usuario;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Objects;

public final class AdminUsuarioTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {"ID", "Nombre", "Correo", "Rol", "Teléfono", "Estado"};
    private List<Usuario> usuarios = List.of();

    @Override
    public int getRowCount() {
        return usuarios.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNAS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 0 ? Long.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Usuario usuario = usuarios.get(row);
        return switch (column) {
            case 0 -> usuario.getId();
            case 1 -> usuario.getNombreCompleto();
            case 2 -> usuario.getEmail();
            case 3 -> usuario.getRol() == null ? "" : usuario.getRol().getNombre();
            case 4 -> usuario.getTelefono() == null ? "" : usuario.getTelefono();
            case 5 -> usuario.isActivo() ? "Activo" : "Inactivo";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public Usuario getUsuario(int row) {
        return usuarios.get(row);
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = List.copyOf(Objects.requireNonNull(usuarios, "usuarios"));
        fireTableDataChanged();
    }
}
