package com.mdp.eventos.view.components;

import com.mdp.eventos.dto.ReservaAdminDTO;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public final class AdminReservaTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {
        "Código", "Cliente", "Correo", "Establecimiento", "Salón",
        "Fecha", "Horario", "Invitados", "Total", "Estado", "Motivo / atención"
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private List<ReservaAdminDTO> reservas = List.of();

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
            case 7 -> Integer.class;
            case 8 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        ReservaAdminDTO reserva = reservas.get(row);
        return switch (column) {
            case 0 -> reserva.codigo();
            case 1 -> reserva.cliente();
            case 2 -> reserva.emailCliente();
            case 3 -> reserva.establecimiento();
            case 4 -> reserva.salon();
            case 5 -> reserva.fecha().format(DATE_FORMAT);
            case 6 -> reserva.horaInicio() + "–" + reserva.horaFin()
                + (reserva.horaFin().isBefore(reserva.horaInicio()) ? " (+1 día)" : "");
            case 7 -> reserva.invitados();
            case 8 -> reserva.total();
            case 9 -> reserva.estado();
            case 10 -> reserva.motivoEstado() == null ? "" : reserva.motivoEstado();
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public void setReservas(List<ReservaAdminDTO> reservas) {
        this.reservas = List.copyOf(Objects.requireNonNull(reservas, "reservas"));
        fireTableDataChanged();
    }

    public ReservaAdminDTO getReserva(int row) {
        return reservas.get(row);
    }
}
