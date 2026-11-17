package com.mdp.eventos.view.components;

import com.mdp.eventos.model.ServicioAdicional;

import javax.swing.table.AbstractTableModel;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ServicioProveedorTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {"Nombre", "Descripción", "Precio", "Modalidad", "Estado"};
    private static final NumberFormat MONEDA =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private List<ServicioAdicional> servicios = List.of();

    @Override
    public int getRowCount() { return servicios.size(); }
    @Override
    public int getColumnCount() { return COLUMNAS.length; }
    @Override
    public String getColumnName(int column) { return COLUMNAS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        ServicioAdicional servicio = servicios.get(row);
        return switch (column) {
            case 0 -> servicio.getNombre();
            case 1 -> servicio.getDescripcion() == null ? "" : servicio.getDescripcion();
            case 2 -> MONEDA.format(servicio.getPrecio());
            case 3 -> servicio.isCobroPorInvitado() ? "Por invitado" : "Por evento";
            case 4 -> servicio.isActivo() ? "Activo" : "Inactivo";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public void setServicios(List<ServicioAdicional> servicios) {
        this.servicios = List.copyOf(Objects.requireNonNull(servicios, "servicios"));
        fireTableDataChanged();
    }

    public ServicioAdicional getServicio(int row) { return servicios.get(row); }
}
