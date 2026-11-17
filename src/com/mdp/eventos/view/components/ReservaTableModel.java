package com.mdp.eventos.view.components;

import com.mdp.eventos.dto.ResumenReservaDTO;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ReservaTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {
        "Código", "Establecimiento", "Salón", "Fecha", "Horario",
        "Invitados", "Total", "Estado"
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private List<ResumenReservaDTO> reservas = List.of();

    @Override
    public int getRowCount() {
        return reservas.size();
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
            case 5 -> Integer.class;
            case 6 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        ResumenReservaDTO reserva = reservas.get(row);
        return switch (column) {
            case 0 -> reserva.codigoReserva();
            case 1 -> reserva.establecimiento();
            case 2 -> reserva.salon();
            case 3 -> reserva.fechaEvento().format(DATE_FORMAT);
            case 4 -> reserva.horaInicio() + "–" + reserva.horaFin()
                + (reserva.horaFin().isBefore(reserva.horaInicio()) ? " (+1 día)" : "");
            case 5 -> reserva.cantidadInvitados();
            case 6 -> reserva.montoTotal();
            case 7 -> reserva.estado();
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public ResumenReservaDTO getReserva(int row) {
        return reservas.get(row);
    }

    public void setReservas(List<ResumenReservaDTO> reservas) {
        this.reservas = List.copyOf(Objects.requireNonNull(reservas, "reservas"));
        fireTableDataChanged();
    }

    public static String formatear(BigDecimal amount) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(amount);
    }
}
