package com.mdp.eventos.view.proveedor;

import com.mdp.eventos.controller.ProveedorController;
import com.mdp.eventos.dto.ProveedorCatalogosDTO;
import com.mdp.eventos.model.Establecimiento;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;
import com.mdp.eventos.view.components.EstablecimientoTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class GestionEstablecimientosView extends JFrame {
    private JPanel rootPanel = new JPanel(new GridBagLayout());
    private JButton btnActualizar;
    private JButton btnNuevoEstablecimiento;
    private JButton btnEditarEstablecimiento;
    private JButton btnCambiarEstado;
    private JButton btnAgregarSalon;
    private JButton btnGestionarSalones;
    private JButton btnGestionarReservas;
    private JTable tblEstablecimientos;
    private JLabel lblEstado;
    private JTextField txtBuscar;
    private JComboBox<String> cmbEstado;
    private ProveedorController controller;
    private final EstablecimientoTableModel tableModel = new EstablecimientoTableModel();
    private TableRowSorter<EstablecimientoTableModel> rowSorter;

    public GestionEstablecimientosView() {
        this(() -> { });
    }

    public GestionEstablecimientosView(Runnable onLogout) {
        super("Eventos MDP | Establecimientos");
        ThemeMdp.styleWindow(rootPanel);
        construirInterfaz(onLogout);
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1120, 720);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz(Runnable onLogout) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        c.gridy = 0;
        rootPanel.add(ThemeMdp.createHeader("Mis establecimientos", onLogout), c);

        JPanel filters = new JPanel(new BorderLayout(10, 0));
        filters.setBackground(java.awt.Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(false);
        search.add(new JLabel("Buscar"), BorderLayout.WEST);
        txtBuscar = new JTextField(28);
        txtBuscar.setToolTipText("Buscar por nombre comercial, CUIT o dirección");
        search.add(txtBuscar, BorderLayout.CENTER);
        filters.add(search, BorderLayout.CENTER);
        JPanel state = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        state.setOpaque(false);
        state.add(new JLabel("Estado"));
        cmbEstado = new JComboBox<>(new String[]{"Todos", "Activo", "Inactivo"});
        state.add(cmbEstado);
        filters.add(state, BorderLayout.EAST);
        c.gridy = 1;
        rootPanel.add(filters, c);

        tblEstablecimientos = new JTable(tableModel);
        ThemeMdp.styleTable(tblEstablecimientos);
        tblEstablecimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEstablecimientos.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeCellRenderer());
        rowSorter = new TableRowSorter<>(tableModel);
        tblEstablecimientos.setRowSorter(rowSorter);
        JScrollPane scroll = new JScrollPane(tblEstablecimientos);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        c.gridy = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        rootPanel.add(scroll, c);

        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setBackground(new java.awt.Color(0xED, 0xF2, 0xF3));
        actions.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(0xCB, 0xD6, 0xDB)),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        lblEstado = new JLabel("Mis establecimientos registrados.", SwingConstants.LEFT);
        actions.add(lblEstado, BorderLayout.CENTER);
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);
        btnNuevoEstablecimiento = new JButton("Nuevo establecimiento");
        btnEditarEstablecimiento = new JButton("Editar");
        btnCambiarEstado = new JButton("Activar / desactivar");
        btnAgregarSalon = new JButton("Agregar salón");
        btnGestionarSalones = new JButton("Gestionar salones");
        btnGestionarReservas = new JButton("Gestionar reservas");
        btnActualizar = new JButton("Actualizar");
        ThemeMdp.stylePrimaryButton(btnNuevoEstablecimiento);
        ThemeMdp.styleSecondaryButton(btnEditarEstablecimiento);
        ThemeMdp.styleSecondaryButton(btnCambiarEstado);
        ThemeMdp.stylePrimaryButton(btnAgregarSalon);
        ThemeMdp.styleSecondaryButton(btnGestionarSalones);
        ThemeMdp.styleSecondaryButton(btnGestionarReservas);
        actionButtons.add(btnNuevoEstablecimiento);
        actionButtons.add(btnEditarEstablecimiento);
        actionButtons.add(btnCambiarEstado);
        actionButtons.add(btnAgregarSalon);
        actionButtons.add(btnGestionarSalones);
        actionButtons.add(btnGestionarReservas);
        actionButtons.add(btnActualizar);
        actions.add(actionButtons, BorderLayout.EAST);
        c.gridy = 3;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 0, 0);
        rootPanel.add(actions, c);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                filtrar();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                filtrar();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                filtrar();
            }
        });
        cmbEstado.addActionListener(event -> filtrar());
    }

    public void setController(ProveedorController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
        btnActualizar.addActionListener(event -> cargarEstablecimientos());
        btnNuevoEstablecimiento.addActionListener(event -> cargarCatalogos(true, null));
        btnEditarEstablecimiento.addActionListener(event -> {
            Establecimiento selected = obtenerSeleccionado();
            if (selected == null) {
                mostrarError("Selecciona el establecimiento que quieres editar.");
                return;
            }
            cargarCatalogos(true, selected);
        });
        btnCambiarEstado.addActionListener(event -> cambiarEstadoSeleccionado());
        btnAgregarSalon.addActionListener(event -> cargarCatalogos(false));
        btnGestionarSalones.addActionListener(event -> {
            GestionSalonesDialog dialog = new GestionSalonesDialog(this, controller);
            dialog.setVisible(true);
        });
        btnGestionarReservas.addActionListener(event -> mostrarGestionReservas());
        cargarEstablecimientos();
    }

    private GestionReservasProveedorDialog reservasDialog;

    private void mostrarGestionReservas() {
        if (reservasDialog == null || !reservasDialog.isDisplayable()) {
            reservasDialog = new GestionReservasProveedorDialog(this, controller);
        }
        reservasDialog.setVisible(true);
        reservasDialog.toFront();
    }

    private void cargarEstablecimientos() {
        btnActualizar.setEnabled(false);
        btnNuevoEstablecimiento.setEnabled(false);
        btnAgregarSalon.setEnabled(false);
        btnGestionarSalones.setEnabled(false);
        btnGestionarReservas.setEnabled(false);
        lblEstado.setText("Consultando establecimientos de MySQL...");
        controller.cargarEstablecimientos(this::mostrarEstablecimientos, this::mostrarError);
    }

    private void cargarCatalogos(boolean crearEstablecimiento) {
        cargarCatalogos(crearEstablecimiento, null);
    }

    private void cargarCatalogos(boolean crearEstablecimiento, Establecimiento editar) {
        btnNuevoEstablecimiento.setEnabled(false);
        btnAgregarSalon.setEnabled(false);
        lblEstado.setText("Cargando datos desde MySQL...");
        controller.cargarCatalogos(
            catalogos -> {
                btnNuevoEstablecimiento.setEnabled(true);
                btnAgregarSalon.setEnabled(true);
                btnGestionarSalones.setEnabled(true);
                btnGestionarReservas.setEnabled(true);
                if (crearEstablecimiento) mostrarFormularioEstablecimiento(catalogos, editar);
                else mostrarFormularioSalon(catalogos);
            },
            mensaje -> {
                btnNuevoEstablecimiento.setEnabled(true);
                btnAgregarSalon.setEnabled(true);
                btnGestionarSalones.setEnabled(true);
                btnGestionarReservas.setEnabled(true);
                mostrarError(mensaje);
            }
        );
    }

    private void mostrarFormularioEstablecimiento(
        ProveedorCatalogosDTO catalogos, Establecimiento original
    ) {
        if (catalogos.zonas().isEmpty()) {
            mostrarError("No hay zonas configuradas. Solicita al administrador que cargue zonas en MySQL.");
            return;
        }
        JTextField nombre = new JTextField(24);
        JTextField cuit = new JTextField(24);
        JTextField direccion = new JTextField(24);
        JComboBox<ZonaMdp> zona = new JComboBox<>(catalogos.zonas().toArray(ZonaMdp[]::new));
        if (original != null) {
            nombre.setText(original.getNombreComercial());
            cuit.setText(original.getCuit());
            direccion.setText(original.getDireccion());
            for (int index = 0; index < zona.getItemCount(); index++) {
                if (zona.getItemAt(index).getId() == original.getZonaId()) {
                    zona.setSelectedIndex(index);
                    break;
                }
            }
        }
        zona.setRenderer((list, value, index, selected, focused) ->
            new JLabel(value == null ? "" : value.getNombre()));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(5, 5, 5, 10);
        form.add(new JLabel("Nombre comercial"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(nombre, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("CUIT"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(cuit, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Dirección"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(direccion, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Zona"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(zona, c);

        if (JOptionPane.showConfirmDialog(
            this, form, original == null ? "Registrar establecimiento" : "Editar establecimiento",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        ) != JOptionPane.OK_OPTION) return;

        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setNombreComercial(nombre.getText());
        establecimiento.setCuit(cuit.getText());
        establecimiento.setDireccion(direccion.getText());
        if (original != null) establecimiento.setId(original.getId());
        ZonaMdp selectedZone = (ZonaMdp) zona.getSelectedItem();
        if (selectedZone == null) {
            mostrarError("Seleccione una zona de Mar del Plata.");
            return;
        }
        establecimiento.setZonaId(selectedZone.getId());
        lblEstado.setText(original == null ? "Registrando establecimiento..." : "Actualizando establecimiento...");
        btnNuevoEstablecimiento.setEnabled(false);
        btnAgregarSalon.setEnabled(false);
        Runnable success = () -> {
            btnNuevoEstablecimiento.setEnabled(true);
            btnAgregarSalon.setEnabled(true);
            btnGestionarSalones.setEnabled(true);
            btnGestionarReservas.setEnabled(true);
            JOptionPane.showMessageDialog(this, original == null
                ? "El establecimiento se registró correctamente."
                : "El establecimiento se actualizó correctamente.");
            cargarEstablecimientos();
        };
        if (original == null) controller.crearEstablecimiento(establecimiento, success, this::mostrarError);
        else controller.editarEstablecimiento(establecimiento, success, this::mostrarError);
    }

    private Establecimiento obtenerSeleccionado() {
        int selectedRow = tblEstablecimientos.getSelectedRow();
        if (selectedRow < 0) return null;
        return tableModel.getDatos().get(tblEstablecimientos.convertRowIndexToModel(selectedRow));
    }

    private void cambiarEstadoSeleccionado() {
        Establecimiento selected = obtenerSeleccionado();
        if (selected == null) {
            mostrarError("Selecciona un establecimiento.");
            return;
        }
        boolean nuevoEstado = !selected.isActivo();
        String confirmacion = nuevoEstado
            ? "¿Quieres activar este establecimiento?"
            : "¿Quieres desactivarlo? Dejará de aparecer en las búsquedas. Las reservas existentes no se borran.";
        if (JOptionPane.showConfirmDialog(this, confirmacion, "Confirmar cambio",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        btnCambiarEstado.setEnabled(false);
        controller.cambiarEstadoEstablecimiento(
            selected.getId(),
            nuevoEstado,
            () -> {
                btnCambiarEstado.setEnabled(true);
                cargarEstablecimientos();
            },
            this::mostrarError
        );
    }

    private void mostrarFormularioSalon(ProveedorCatalogosDTO catalogos) {
        List<Establecimiento> propiosActivos = tableModel.getDatos().stream()
            .filter(Establecimiento::isActivo)
            .toList();
        if (propiosActivos.isEmpty()) {
            mostrarError("Primero registra o activa un establecimiento para poder agregarle salones.");
            return;
        }
        if (catalogos.tiposEvento().isEmpty()) {
            mostrarError("No hay tipos de evento configurados en MySQL.");
            return;
        }
        JComboBox<Establecimiento> establecimiento =
            new JComboBox<>(propiosActivos.toArray(Establecimiento[]::new));
        establecimiento.setRenderer((list, value, index, selected, focused) ->
            new JLabel(value == null ? "" : value.getNombreComercial()));
        JTextField nombre = new JTextField("Salón principal", 18);
        JTextField capacidadMinima = new JTextField("20", 8);
        JTextField capacidadMaxima = new JTextField("150", 8);
        JTextField apertura = new JTextField("18:00", 8);
        JTextField cierre = new JTextField("03:00", 8);
        JTextField tarifaHora = new JTextField("0.00", 8);
        JTextField costoPersona = new JTextField("0.00", 8);
        JList<TipoEvento> tipos = new JList<>(catalogos.tiposEvento().toArray(TipoEvento[]::new));
        tipos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tipos.setVisibleRowCount(Math.min(4, catalogos.tiposEvento().size()));
        tipos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused
            ) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setText(((TipoEvento) value).getNombre());
                return this;
            }
        });
        JList<String> dias = new JList<>(new String[]{
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"
        });
        dias.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dias.setVisibleRowCount(5);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(4, 5, 4, 8);
        form.add(new JLabel("Establecimiento"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(establecimiento, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Nombre del salón"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(nombre, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Capacidad mínima"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(capacidadMinima, c);
        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("máxima"), c);
        c.gridx = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(capacidadMaxima, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Apertura (HH:mm)"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(apertura, c);
        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Cierre"), c);
        c.gridx = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(cierre, c);
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Tarifa por hora"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tarifaHora, c);
        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Costo por persona"), c);
        c.gridx = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(costoPersona, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Tipos de evento"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(new JScrollPane(tipos), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        form.add(new JLabel("Días disponibles"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(new JScrollPane(dias), c);

        if (JOptionPane.showConfirmDialog(
            this, form, "Agregar salón", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        ) != JOptionPane.OK_OPTION) return;

        try {
            Salon salon = new Salon();
            Establecimiento selected = (Establecimiento) establecimiento.getSelectedItem();
            if (selected == null) {
                mostrarError("Seleccione un establecimiento propio activo.");
                return;
            }
            salon.setEstablecimientoId(selected.getId());
            salon.setNombreSalon(nombre.getText());
            salon.setCapacidadMinima(Integer.parseInt(capacidadMinima.getText().trim()));
            salon.setCapacidadMaxima(Integer.parseInt(capacidadMaxima.getText().trim()));
            salon.setHoraApertura(LocalTime.parse(apertura.getText().trim()));
            salon.setHoraCierre(LocalTime.parse(cierre.getText().trim()));
            salon.setTarifaBaseHora(new BigDecimal(tarifaHora.getText().trim().replace(',', '.')));
            salon.setCostoPorPersona(new BigDecimal(costoPersona.getText().trim().replace(',', '.')));
            List<Long> tiposSeleccionados = tipos.getSelectedValuesList().stream().map(TipoEvento::getId).toList();
            List<String> diasSeleccionados = dias.getSelectedValuesList();
            lblEstado.setText("Guardando salón, tipos de evento y días...");
            btnNuevoEstablecimiento.setEnabled(false);
            btnAgregarSalon.setEnabled(false);
            controller.crearSalon(
                salon,
                tiposSeleccionados,
                diasSeleccionados,
                () -> {
                    btnNuevoEstablecimiento.setEnabled(true);
                    btnEditarEstablecimiento.setEnabled(true);
                    btnCambiarEstado.setEnabled(true);
                    btnAgregarSalon.setEnabled(true);
                    btnGestionarSalones.setEnabled(true);
                    btnGestionarReservas.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "El salón se agregó correctamente.");
                    cargarEstablecimientos();
                },
                this::mostrarError
            );
        } catch (NumberFormatException | java.time.format.DateTimeParseException exception) {
            mostrarError("Revisa las capacidades, los importes y los horarios. Usa HH:mm para las horas.");
        }
    }

    private void mostrarEstablecimientos(List<Establecimiento> establecimientos) {
        btnActualizar.setEnabled(true);
        btnNuevoEstablecimiento.setEnabled(true);
        btnEditarEstablecimiento.setEnabled(true);
        btnCambiarEstado.setEnabled(true);
        btnAgregarSalon.setEnabled(true);
        btnGestionarReservas.setEnabled(true);
        tableModel.setDatos(establecimientos);
        filtrar();
    }

    private void filtrar() {
        if (rowSorter == null) return;
        String query = txtBuscar.getText().trim().toLowerCase(java.util.Locale.ROOT);
        String status = (String) cmbEstado.getSelectedItem();
        rowSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends EstablecimientoTableModel, ? extends Integer> entry) {
                boolean matchesText = query.isEmpty()
                    || entry.getStringValue(0).toLowerCase(java.util.Locale.ROOT).contains(query)
                    || entry.getStringValue(1).toLowerCase(java.util.Locale.ROOT).contains(query)
                    || entry.getStringValue(2).toLowerCase(java.util.Locale.ROOT).contains(query);
                boolean active = Boolean.TRUE.equals(entry.getValue(3));
                boolean matchesStatus = "Todos".equals(status)
                    || ("Activo".equals(status) && active)
                    || ("Inactivo".equals(status) && !active);
                return matchesText && matchesStatus;
            }
        });
        lblEstado.setText("Mostrando " + tblEstablecimientos.getRowCount()
            + " de " + tableModel.getRowCount() + " establecimientos de MySQL.");
    }

    private void mostrarError(String mensaje) {
        btnActualizar.setEnabled(true);
        btnNuevoEstablecimiento.setEnabled(true);
        btnAgregarSalon.setEnabled(true);
        lblEstado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
