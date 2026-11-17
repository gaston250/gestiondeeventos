package com.mdp.eventos.view.components;

import com.mdp.eventos.dto.CatalogoAdminDTO;

import javax.swing.table.AbstractTableModel;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CatalogoAdminTableModel extends AbstractTableModel {
    private static final String[] COLUMNAS = {
        "Nombre", "Descripción", "Código postal", "Precio sugerido", "Modalidad", "Estado"
    };
    private static final NumberFormat MONEDA =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private List<CatalogoAdminDTO> datos = List.of();

    @Override
    public int getRowCount() { return datos.size(); }
    @Override
    public int getColumnCount() { return COLUMNAS.length; }
    @Override
    public String getColumnName(int column) { return COLUMNAS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        CatalogoAdminDTO dto = datos.get(row);
        return switch (column) {
            case 0 -> dto.nombre();
            case 1 -> dto.descripcion() == null ? "" : dto.descripcion();
            case 2 -> dto.codigoPostal() == null ? "" : dto.codigoPostal();
            case 3 -> dto.precio() == null ? "" : MONEDA.format(dto.precio());
            case 4 -> dto.tipo() != com.mdp.eventos.model.TipoCatalogoAdmin.SERVICIO_BASE
                ? "" : dto.cobroPorInvitado() ? "Por invitado" : "Por evento";
            case 5 -> dto.activo() ? "Activo" : "Inactivo";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + column);
        };
    }

    public void setDatos(List<CatalogoAdminDTO> datos) {
        this.datos = List.copyOf(Objects.requireNonNull(datos, "datos"));
        fireTableDataChanged();
    }

    public CatalogoAdminDTO getDato(int row) { return datos.get(row); }
}
