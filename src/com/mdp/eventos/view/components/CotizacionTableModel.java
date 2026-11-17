package com.mdp.eventos.view.components;

import com.mdp.eventos.dto.CotizacionSalonDTO;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CotizacionTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {
        "Establecimiento", "Salón", "Zona y Dirección", "Cap. Máxima",
        "Invitados", "Horario", "Precio Total"
    };

    private List<CotizacionSalonDTO> datos = List.of();
    private List<CotizacionSalonDTO> datosOriginales = List.of();

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
        return switch (columnIndex) {
            case 3, 4 -> Integer.class;
            case 6 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CotizacionSalonDTO fila = datos.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> fila.establecimiento();
            case 1 -> fila.salon();
            case 2 -> fila.zona() + " - " + fila.direccion();
            case 3 -> fila.capacidadMaxima();
            case 4 -> fila.invitados();
            case 5 -> fila.horaInicio() + " - " + fila.horaFin()
                + (fila.horaFin().isBefore(fila.horaInicio()) ? " (+1 día)" : "");
            case 6 -> fila.precioTotalEstimado();
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columnIndex);
        };
    }

    public CotizacionSalonDTO getFila(int rowIndex) {
        return datos.get(rowIndex);
    }

    public void setDatos(List<CotizacionSalonDTO> nuevosDatos) {
        datosOriginales = List.copyOf(Objects.requireNonNull(nuevosDatos, "nuevosDatos"));
        datos = datosOriginales;
        fireTableDataChanged();
    }

    public void ordenarPor(String criterio) {
        Objects.requireNonNull(criterio, "criterio");
        Comparator<CotizacionSalonDTO> comparator = switch (criterio) {
            case "Menor tarifa" -> Comparator.comparing(CotizacionSalonDTO::precioTotalEstimado)
                .thenComparing(CotizacionSalonDTO::establecimiento)
                .thenComparing(CotizacionSalonDTO::salon);
            case "Capacidad" -> Comparator.comparingInt(CotizacionSalonDTO::capacidadMaxima)
                .thenComparing(CotizacionSalonDTO::precioTotalEstimado);
            default -> null;
        };
        if (comparator == null) {
            datos = datosOriginales;
        } else {
            datos = datosOriginales.stream().sorted(comparator).toList();
        }
        fireTableDataChanged();
    }

    public void limpiar() {
        setDatos(List.of());
    }
}
