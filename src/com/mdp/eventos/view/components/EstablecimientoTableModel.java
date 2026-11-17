package com.mdp.eventos.view.components;

import com.mdp.eventos.model.Establecimiento;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Objects;

public class EstablecimientoTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {"Nombre Comercial", "CUIT", "Dirección", "Activo"};
    private List<Establecimiento> datos = List.of();

    @Override
    public int getRowCount() {
        return datos.size();
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
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 3 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Establecimiento establecimiento = datos.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> establecimiento.getNombreComercial();
            case 1 -> establecimiento.getCuit();
            case 2 -> establecimiento.getDireccion();
            case 3 -> establecimiento.isActivo();
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columnIndex);
        };
    }

    public void setDatos(List<Establecimiento> establecimientos) {
        datos = List.copyOf(Objects.requireNonNull(establecimientos, "establecimientos"));
        fireTableDataChanged();
    }

    public List<Establecimiento> getDatos() {
        return datos;
    }
}
