package com.mdp.eventos.view.proveedor;

import com.mdp.eventos.controller.ProveedorController;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.view.components.ServicioProveedorTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Objects;

public final class GestionServiciosDialog extends JDialog {
    private final ProveedorController controller;
    private final long salonId;
    private final ServicioProveedorTableModel model = new ServicioProveedorTableModel();
    private final JTable tabla = new JTable(model);
    private final JLabel estado = new JLabel("Cargando servicios...");
    private final JButton nuevo = new JButton("Nuevo servicio");
    private final JButton plantilla = new JButton("Importar plantilla");
    private final JButton editar = new JButton("Editar");
    private final JButton cambiarEstado = new JButton("Desactivar");
    private final JButton actualizar = new JButton("Actualizar");

    public GestionServiciosDialog(Window owner, ProveedorController controller, long salonId) {
        super(owner, "Servicios adicionales del salón", Dialog.ModalityType.MODELESS);
        this.controller = Objects.requireNonNull(controller, "controller");
        this.salonId = salonId;
        construir();
        cargar();
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        ThemeMdp.styleWindow(root);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ThemeMdp.styleTable(tabla);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeCellRenderer());
        root.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(estado, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ThemeMdp.stylePrimaryButton(nuevo);
        ThemeMdp.styleSecondaryButton(plantilla);
        ThemeMdp.styleSecondaryButton(editar);
        ThemeMdp.styleSecondaryButton(cambiarEstado);
        ThemeMdp.styleSecondaryButton(actualizar);
        buttons.add(nuevo);
        buttons.add(plantilla);
        buttons.add(editar);
        buttons.add(cambiarEstado);
        buttons.add(actualizar);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(760, 400));
        setSize(900, 480);
        setLocationRelativeTo(getOwner());
        nuevo.addActionListener(event -> formulario(null));
        plantilla.addActionListener(event -> importarPlantilla());
        editar.addActionListener(event -> {
            ServicioAdicional servicio = seleccionado();
            if (servicio == null) mostrarError("Seleccione un servicio para editar.");
            else formulario(servicio);
        });
        cambiarEstado.addActionListener(event -> cambiarEstado());
        actualizar.addActionListener(event -> cargar());
        tabla.getSelectionModel().addListSelectionListener(event -> actualizarAcciones());
    }

    private void importarPlantilla() {
        setCargando(true);
        controller.cargarPlantillasServicio(plantillas -> {
            setCargando(false);
            if (plantillas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay plantillas de servicios activas.",
                    "Sin plantillas", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] opciones = plantillas.stream()
                .map(item -> item.getNombre() + " — $" + item.getPrecio().toPlainString())
                .toArray(String[]::new);
            String seleccion = (String) JOptionPane.showInputDialog(
                this, "Seleccione una plantilla; se copiará como un servicio editable del salón.",
                "Importar servicio base", JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]
            );
            if (seleccion == null) return;
            int index = java.util.Arrays.asList(opciones).indexOf(seleccion);
            if (index < 0) return;
            setCargando(true);
            controller.agregarPlantillaServicio(
                plantillas.get(index).getId(), salonId, this::cargar, this::mostrarError
            );
        }, mensaje -> {
            setCargando(false);
            mostrarError(mensaje);
        });
    }

    private void cargar() {
        setCargando(true);
        estado.setText("Consultando servicios de MySQL...");
        controller.cargarServicios(salonId, servicios -> {
            model.setServicios(servicios);
            estado.setText(servicios.isEmpty()
                ? "Este salón todavía no tiene servicios adicionales."
                : servicios.size() + " servicios; los inactivos no aparecen en cotizaciones.");
            setCargando(false);
        }, mensaje -> {
            estado.setText("No se pudieron cargar los servicios.");
            setCargando(false);
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void formulario(ServicioAdicional original) {
        JTextField nombre = new JTextField(original == null ? "" : original.getNombre(), 22);
        JTextField descripcion = new JTextField(
            original == null || original.getDescripcion() == null ? "" : original.getDescripcion(), 22
        );
        JTextField precio = new JTextField(original == null ? "" : original.getPrecio().toPlainString(), 12);
        JCheckBox porInvitado = new JCheckBox("Cobrar por invitado", original != null && original.isCobroPorInvitado());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nombre"));
        form.add(nombre);
        form.add(new JLabel("Descripción"));
        form.add(descripcion);
        form.add(new JLabel("Precio (> 0)"));
        form.add(precio);
        form.add(new JLabel("Modalidad"));
        form.add(porInvitado);
        if (JOptionPane.showConfirmDialog(this, form,
            original == null ? "Nuevo servicio" : "Editar servicio",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            ServicioAdicional servicio = original == null ? new ServicioAdicional() : original;
            servicio.setSalonId(salonId);
            servicio.setNombre(nombre.getText());
            servicio.setDescripcion(descripcion.getText());
            servicio.setPrecio(new BigDecimal(precio.getText().trim().replace(',', '.')));
            servicio.setCobroPorInvitado(porInvitado.isSelected());
            setCargando(true);
            if (original == null) controller.crearServicio(servicio, this::cargar, this::mostrarError);
            else controller.editarServicio(servicio, this::cargar, this::mostrarError);
        } catch (NumberFormatException exception) {
            mostrarError("Ingrese un precio válido y mayor que cero.");
        }
    }

    private void cambiarEstado() {
        ServicioAdicional servicio = seleccionado();
        if (servicio == null) return;
        boolean activo = !servicio.isActivo();
        if (JOptionPane.showConfirmDialog(this,
            activo ? "¿Activar este servicio?" : "¿Desactivar este servicio? Las reservas existentes conservarán su precio.",
            "Confirmar cambio", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        setCargando(true);
        controller.cambiarEstadoServicio(servicio.getId(), salonId, activo, this::cargar, this::mostrarError);
    }

    private ServicioAdicional seleccionado() {
        int row = tabla.getSelectedRow();
        return row < 0 ? null : model.getServicio(tabla.convertRowIndexToModel(row));
    }

    private void actualizarAcciones() {
        ServicioAdicional item = seleccionado();
        boolean selected = item != null && actualizar.isEnabled();
        editar.setEnabled(selected);
        cambiarEstado.setEnabled(selected);
        cambiarEstado.setText(item != null && item.isActivo() ? "Desactivar" : "Activar");
    }

    private void setCargando(boolean cargando) {
        nuevo.setEnabled(!cargando);
        plantilla.setEnabled(!cargando);
        actualizar.setEnabled(!cargando);
        tabla.setEnabled(!cargando);
        if (cargando) {
            editar.setEnabled(false);
            cambiarEstado.setEnabled(false);
        } else actualizarAcciones();
    }

    private void mostrarError(String mensaje) {
        setCargando(false);
        estado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, mensaje, "Validación", JOptionPane.WARNING_MESSAGE);
    }
}
