package com.mdp.eventos.view.proveedor;

import com.mdp.eventos.controller.ProveedorController;
import com.mdp.eventos.dto.ProveedorCatalogosDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.view.components.SalonProveedorTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public final class GestionSalonesDialog extends JDialog {
    private final ProveedorController controller;
    private final SalonProveedorTableModel model = new SalonProveedorTableModel();
    private final JTable tabla = new JTable(model);
    private final JLabel estado = new JLabel("Cargando salones...");
    private final JButton editar = new JButton("Editar");
    private final JButton servicios = new JButton("Servicios");
    private final JButton cambiarEstado = new JButton("Desactivar");
    private final JButton actualizar = new JButton("Actualizar");

    public GestionSalonesDialog(Window owner, ProveedorController controller) {
        super(owner, "Gestionar salones", Dialog.ModalityType.MODELESS);
        this.controller = Objects.requireNonNull(controller, "controller");
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
        ThemeMdp.styleSecondaryButton(editar);
        ThemeMdp.styleSecondaryButton(servicios);
        ThemeMdp.styleDangerButton(cambiarEstado);
        ThemeMdp.styleSecondaryButton(actualizar);
        buttons.add(editar);
        buttons.add(servicios);
        buttons.add(cambiarEstado);
        buttons.add(actualizar);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(820, 420));
        setSize(980, 520);
        setLocationRelativeTo(getOwner());
        editar.addActionListener(event -> editarSeleccionado());
        servicios.addActionListener(event -> gestionarServicios());
        cambiarEstado.addActionListener(event -> cambiarEstadoSeleccionado());
        actualizar.addActionListener(event -> cargar());
        tabla.getSelectionModel().addListSelectionListener(event -> actualizarAcciones());
    }

    private void cargar() {
        setCargando(true);
        estado.setText("Consultando salones propios en MySQL...");
        controller.cargarSalones(salones -> {
            model.setSalones(salones);
            estado.setText(salones.isEmpty()
                ? "Todavía no hay salones registrados."
                : salones.size() + " salones registrados.");
            setCargando(false);
        }, mensaje -> {
            estado.setText("No se pudieron cargar los salones.");
            setCargando(false);
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void editarSeleccionado() {
        SalonProveedorDTO item = seleccionado();
        if (item == null) return;
        setCargando(true);
        controller.cargarCatalogos(catalogos -> {
            setCargando(false);
            mostrarFormulario(item, catalogos);
        }, mensaje -> {
            setCargando(false);
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void gestionarServicios() {
        SalonProveedorDTO item = seleccionado();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un salón propio.", "Selección requerida",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        new GestionServiciosDialog(this, controller, item.salon().getId()).setVisible(true);
    }

    private void mostrarFormulario(SalonProveedorDTO item, ProveedorCatalogosDTO catalogos) {
        Salon salon = item.salon();
        JTextField nombre = new JTextField(salon.getNombreSalon(), 18);
        JTextField capacidadMinima = new JTextField(Integer.toString(salon.getCapacidadMinima()), 8);
        JTextField capacidadMaxima = new JTextField(Integer.toString(salon.getCapacidadMaxima()), 8);
        JTextField apertura = new JTextField(salon.getHoraApertura().toString(), 8);
        JTextField cierre = new JTextField(salon.getHoraCierre().toString(), 8);
        JTextField tarifa = new JTextField(salon.getTarifaBaseHora().toPlainString(), 8);
        JTextField persona = new JTextField(salon.getCostoPorPersona().toPlainString(), 8);
        JList<TipoEvento> tipos = new JList<>(catalogos.tiposEvento().toArray(TipoEvento[]::new));
        tipos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tipos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus
            ) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(((TipoEvento) value).getNombre());
                return this;
            }
        });
        for (int i = 0; i < catalogos.tiposEvento().size(); i++) {
            if (item.tiposEventoIds().contains(catalogos.tiposEvento().get(i).getId())) tipos.addSelectionInterval(i, i);
        }
        String[] nombresDias = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};
        JList<String> dias = new JList<>(nombresDias);
        dias.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < nombresDias.length; i++) {
            if (item.dias().contains(nombresDias[i])) dias.addSelectionInterval(i, i);
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nombre"));
        form.add(nombre);
        form.add(new JLabel("Capacidad mínima"));
        form.add(capacidadMinima);
        form.add(new JLabel("Capacidad máxima"));
        form.add(capacidadMaxima);
        form.add(new JLabel("Apertura (HH:mm)"));
        form.add(apertura);
        form.add(new JLabel("Cierre (HH:mm; puede cruzar medianoche)"));
        form.add(cierre);
        form.add(new JLabel("Tarifa por hora (> 0)"));
        form.add(tarifa);
        form.add(new JLabel("Costo por persona (> 0)"));
        form.add(persona);
        form.add(new JLabel("Tipos de evento"));
        form.add(new JScrollPane(tipos));
        form.add(new JLabel("Días disponibles"));
        form.add(new JScrollPane(dias));
        if (JOptionPane.showConfirmDialog(this, form, "Editar salón",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            salon.setNombreSalon(nombre.getText());
            salon.setCapacidadMinima(Integer.parseInt(capacidadMinima.getText().trim()));
            salon.setCapacidadMaxima(Integer.parseInt(capacidadMaxima.getText().trim()));
            salon.setHoraApertura(LocalTime.parse(apertura.getText().trim()));
            salon.setHoraCierre(LocalTime.parse(cierre.getText().trim()));
            salon.setTarifaBaseHora(new BigDecimal(tarifa.getText().trim().replace(',', '.')));
            salon.setCostoPorPersona(new BigDecimal(persona.getText().trim().replace(',', '.')));
            List<Long> ids = tipos.getSelectedValuesList().stream().map(TipoEvento::getId).toList();
            controller.editarSalon(salon, ids, dias.getSelectedValuesList(), this::cargar, this::mostrarError);
            setCargando(true);
        } catch (NumberFormatException | java.time.format.DateTimeParseException exception) {
            mostrarError("Revisa capacidades, importes y horarios. Usa HH:mm y tarifas mayores que cero.");
        }
    }

    private void cambiarEstadoSeleccionado() {
        SalonProveedorDTO item = seleccionado();
        if (item == null) return;
        boolean activar = !item.salon().isActivo();
        String mensaje = activar ? "¿Activar este salón?" : "¿Desactivar este salón?";
        if (JOptionPane.showConfirmDialog(this, mensaje, "Confirmar cambio",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        setCargando(true);
        controller.cambiarEstadoSalon(item.salon().getId(), activar, this::cargar, this::mostrarError);
    }

    private SalonProveedorDTO seleccionado() {
        int row = tabla.getSelectedRow();
        return row < 0 ? null : model.getSalon(tabla.convertRowIndexToModel(row));
    }

    private void actualizarAcciones() {
        SalonProveedorDTO item = seleccionado();
        boolean seleccionado = item != null && actualizar.isEnabled();
        editar.setEnabled(seleccionado);
        servicios.setEnabled(seleccionado);
        cambiarEstado.setEnabled(seleccionado);
        cambiarEstado.setText(item != null && item.salon().isActivo() ? "Desactivar" : "Activar");
    }

    private void setCargando(boolean cargando) {
        actualizar.setEnabled(!cargando);
        tabla.setEnabled(!cargando);
        if (cargando) {
            editar.setEnabled(false);
            servicios.setEnabled(false);
            cambiarEstado.setEnabled(false);
        } else {
            actualizarAcciones();
        }
    }

    private void mostrarError(String mensaje) {
        setCargando(false);
        estado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo actualizar el salón",
            JOptionPane.WARNING_MESSAGE);
    }
}
