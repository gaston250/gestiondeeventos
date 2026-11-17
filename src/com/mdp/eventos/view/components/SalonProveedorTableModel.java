package com.mdp.eventos.view.components;

import com.mdp.eventos.dto.SalonProveedorDTO;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Objects;

public final class SalonProveedorTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {
        "Salón", "Establecimiento", "Capacidad", "Horario", "Estado"
    };
    private List<SalonProveedorDTO> salones = List.of();

    @Override
    public int getRowCount() {
        return salones.size();
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
    public Object getValueAt(int row, int column) {
        SalonProveedorDTO item = salones.get(row);
        return switch (column) {
            case 0 -> item.salon().getNombreSalon();
            case 1 -> item.establecimiento();
            case 2 -> item.salon().getCapacidadMinima() + "–" + item.salon().getCapacidadMaxima();
            case 3 -> item.salon().getHoraApertura() + "–" + item.salon().getHoraCierre();
            case 4 -> item.salon().isActivo() ? "Activo" : "Inactivo";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public void setSalones(List<SalonProveedorDTO> salones) {
        this.salones = List.copyOf(Objects.requireNonNull(salones, "salones"));
        fireTableDataChanged();
    }

    public SalonProveedorDTO getSalon(int row) {
        return salones.get(row);
    }
}
