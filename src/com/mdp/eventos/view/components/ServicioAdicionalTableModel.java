package com.mdp.eventos.view.components;

import com.mdp.eventos.model.ServicioAdicional;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ServicioAdicionalTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {"Incluir", "Servicio adicional", "Precio", "Modalidad"};
    private final List<ServicioAdicional> servicios = new ArrayList<>();
    private final List<Boolean> seleccionados = new ArrayList<>();
    private int invitados;

    public ServicioAdicionalTableModel(List<ServicioAdicional> servicios, int invitados) {
        this.invitados = invitados;
        this.servicios.addAll(List.copyOf(Objects.requireNonNull(servicios, "servicios")));
        for (int i = 0; i < this.servicios.size(); i++) seleccionados.add(false);
    }

    @Override
    public int getRowCount() {
        return servicios.size();
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
        return switch (column) {
            case 0 -> Boolean.class;
            case 2 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
        ServicioAdicional servicio = servicios.get(row);
        return switch (column) {
            case 0 -> seleccionados.get(row);
            case 1 -> servicio.getNombre()
                + (servicio.getDescripcion() == null || servicio.getDescripcion().isBlank()
                    ? "" : " — " + servicio.getDescripcion());
            case 2 -> importe(servicio);
            case 3 -> servicio.isCobroPorInvitado() ? "Por invitado" : "Por evento";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column != 0 || !(value instanceof Boolean seleccionado)) return;
        seleccionados.set(row, seleccionado);
        fireTableCellUpdated(row, column);
        fireTableCellUpdated(row, 2);
    }

    public BigDecimal totalServicios() {
        BigDecimal total = BigDecimal.ZERO;
        for (int row = 0; row < servicios.size(); row++) {
            if (seleccionados.get(row)) total = total.add(importe(servicios.get(row)));
        }
        return total;
    }

    public List<Long> getServiciosSeleccionadosIds() {
        List<Long> ids = new ArrayList<>();
        for (int row = 0; row < servicios.size(); row++) {
            if (seleccionados.get(row)) ids.add(servicios.get(row).getId());
        }
        return List.copyOf(ids);
    }

    public void setInvitados(int invitados) {
        this.invitados = invitados;
        if (!servicios.isEmpty()) fireTableRowsUpdated(0, getRowCount() - 1);
    }

    private BigDecimal importe(ServicioAdicional servicio) {
        return servicio.isCobroPorInvitado()
            ? servicio.getPrecio().multiply(BigDecimal.valueOf(invitados))
            : servicio.getPrecio();
    }

    public static String formatear(BigDecimal amount) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(amount);
    }
}
