package com.mdp.eventos.view.admin;

import com.mdp.eventos.controller.AdminController;
import com.mdp.eventos.dto.CatalogoAdminDTO;
import com.mdp.eventos.model.TipoCatalogoAdmin;
import com.mdp.eventos.view.components.CatalogoAdminTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

public final class CatalogosAdminPanel extends JPanel {
    private final JComboBox<TipoCatalogoAdmin> tipo = new JComboBox<>(TipoCatalogoAdmin.values());
    private final CatalogoAdminTableModel model = new CatalogoAdminTableModel();
    private final JTable tabla = new JTable(model);
    private final JLabel estado = new JLabel("Seleccione un catálogo.");
    private final JButton nuevo = new JButton("Nuevo");
    private final JButton editar = new JButton("Editar");
    private final JButton cambiarEstado = new JButton("Desactivar");
    private final JButton actualizar = new JButton("Actualizar");
    private AdminController controller;
    private boolean cargando;

    public CatalogosAdminPanel() {
        super(new BorderLayout(0, 10));
        setBackground(java.awt.Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        construir();
    }

    private void construir() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Catálogo"));
        filters.add(tipo);
        ThemeMdp.styleSecondaryButton(actualizar);
        filters.add(actualizar);
        add(filters, BorderLayout.NORTH);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ThemeMdp.styleTable(tabla);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeCellRenderer());
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(estado, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        ThemeMdp.stylePrimaryButton(nuevo);
        ThemeMdp.styleSecondaryButton(editar);
        ThemeMdp.styleSecondaryButton(cambiarEstado);
        actions.add(nuevo);
        actions.add(editar);
        actions.add(cambiarEstado);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        tipo.addActionListener(event -> cargar());
        actualizar.addActionListener(event -> cargar());
        nuevo.addActionListener(event -> mostrarFormulario(null));
        editar.addActionListener(event -> {
            CatalogoAdminDTO dto = seleccionado();
            if (dto == null) mostrarError("Seleccione un registro para editar.");
            else mostrarFormulario(dto);
        });
        cambiarEstado.addActionListener(event -> cambiarEstado());
        tabla.getSelectionModel().addListSelectionListener(event -> actualizarAcciones());
    }

    public void setController(AdminController controller) {
        this.controller = java.util.Objects.requireNonNull(controller, "controller");
        cargar();
    }

    private void cargar() {
        if (controller == null) return;
        TipoCatalogoAdmin selected = (TipoCatalogoAdmin) tipo.getSelectedItem();
        if (selected == null) return;
        setCargando(true);
        estado.setText("Consultando catálogo en MySQL...");
        controller.cargarCatalogo(selected, datos -> {
            model.setDatos(datos);
            estado.setText(datos.isEmpty()
                ? "No hay registros en este catálogo."
                : datos.size() + " registros. Los registros en uso se desactivan; no se borran.");
            setCargando(false);
        }, mensaje -> {
            setCargando(false);
            estado.setText("No se pudo cargar el catálogo.");
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void mostrarFormulario(CatalogoAdminDTO original) {
        TipoCatalogoAdmin selectedType = (TipoCatalogoAdmin) tipo.getSelectedItem();
        if (selectedType == null) return;
        JTextField nombre = new JTextField(original == null ? "" : original.nombre(), 24);
        JTextField descripcion = new JTextField(
            original == null || original.descripcion() == null ? "" : original.descripcion(), 24
        );
        JTextField postal = new JTextField(
            original == null || original.codigoPostal() == null ? "" : original.codigoPostal(), 10
        );
        JTextField precio = new JTextField(
            original == null || original.precio() == null ? "" : original.precio().toPlainString(), 12
        );
        JCheckBox porInvitado = new JCheckBox(
            "Cobro por invitado", original != null && original.cobroPorInvitado()
        );
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nombre"));
        form.add(nombre);
        if (selectedType != TipoCatalogoAdmin.ZONA) {
            form.add(new JLabel("Descripción"));
            form.add(descripcion);
        }
        if (selectedType == TipoCatalogoAdmin.ZONA) {
            form.add(new JLabel("Código postal"));
            form.add(postal);
        }
        if (selectedType == TipoCatalogoAdmin.SERVICIO_BASE) {
            form.add(new JLabel("Precio sugerido (> 0)"));
            form.add(precio);
            form.add(new JLabel("Modalidad"));
            form.add(porInvitado);
        }
        String title = original == null ? "Nuevo registro" : "Editar registro";
        if (JOptionPane.showConfirmDialog(this, form, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            CatalogoAdminDTO dto = new CatalogoAdminDTO(
                original == null ? 0 : original.id(),
                selectedType,
                nombre.getText(),
                selectedType == TipoCatalogoAdmin.ZONA ? null : descripcion.getText(),
                selectedType == TipoCatalogoAdmin.ZONA ? postal.getText() : null,
                selectedType == TipoCatalogoAdmin.SERVICIO_BASE
                    ? new BigDecimal(precio.getText().trim().replace(',', '.')) : null,
                porInvitado.isSelected(),
                true
            );
            setCargando(true);
            controller.guardarCatalogo(dto, this::cargar, this::mostrarError);
        } catch (NumberFormatException exception) {
            mostrarError("Ingrese un precio válido y mayor que cero.");
        }
    }

    private void cambiarEstado() {
        CatalogoAdminDTO dto = seleccionado();
        if (dto == null) return;
        boolean activo = !dto.activo();
        String message = activo
            ? "¿Activar este registro?"
            : "No se borrará. ¿Desactivar el registro para impedir nuevos usos?";
        if (JOptionPane.showConfirmDialog(this, message, "Confirmar estado",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        setCargando(true);
        controller.cambiarEstadoCatalogo(dto.tipo(), dto.id(), activo, this::cargar, this::mostrarError);
    }

    private CatalogoAdminDTO seleccionado() {
        int row = tabla.getSelectedRow();
        return row < 0 ? null : model.getDato(tabla.convertRowIndexToModel(row));
    }

    private void actualizarAcciones() {
        CatalogoAdminDTO dto = seleccionado();
        editar.setEnabled(!cargando && dto != null);
        cambiarEstado.setEnabled(!cargando && dto != null);
        cambiarEstado.setText(dto != null && dto.activo() ? "Desactivar" : "Activar");
    }

    private void setCargando(boolean value) {
        cargando = value;
        tipo.setEnabled(!value);
        nuevo.setEnabled(!value);
        actualizar.setEnabled(!value);
        tabla.setEnabled(!value);
        actualizarAcciones();
    }

    private void mostrarError(String mensaje) {
        setCargando(false);
        estado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, mensaje, "Validación", JOptionPane.WARNING_MESSAGE);
    }
}
