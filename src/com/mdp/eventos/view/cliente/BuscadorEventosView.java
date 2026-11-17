package com.mdp.eventos.view.cliente;

import com.mdp.eventos.dto.CotizacionSalonDTO;
import com.mdp.eventos.controller.ClienteController;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;
import com.mdp.eventos.view.components.CotizacionTableModel;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class BuscadorEventosView extends JFrame {
    private JPanel rootPanel = new JPanel(new GridBagLayout());
    private JPanel panelFiltros;
    private JComboBox<EventoOpcion> cmbTipoEvento;
    private JSpinner spnInvitados;
    private JTextField txtFecha;
    private JTextField txtHoraInicio;
    private JTextField txtHoraFin;
    private JComboBox<ZonaOpcion> cmbZona;
    private JComboBox<String> cmbOrden;
    private JButton btnBuscar;
    private JScrollPane scrollResultados;
    private JTable tblResultados;
    private JPanel panelAcciones;
    private JLabel lblEstado;
    private JLabel lblHorario;
    private JLabel lblErrorInvitados;
    private JLabel lblAtribucion;
    private JLabel lblUltimaConsulta;
    private JLabel lblResumenSeleccion;
    private JLabel lblResultadosVacios;
    private JProgressBar progressBusqueda;
    private JButton btnLimpiar;
    private JButton btnContinuarReserva;

    private final CotizacionTableModel tableModel;
    private final Runnable onLogoutCallback;
    private Runnable onOpenReservations = () -> { };

    public BuscadorEventosView() {
        this(() -> { });
    }

    public BuscadorEventosView(Runnable onLogout) {
        super("Eventos MDP | Buscar salones");
        onLogoutCallback = java.util.Objects.requireNonNull(onLogout, "onLogout");
        setContentPane(rootPanel);
        ThemeMdp.styleWindow(rootPanel);
        tableModel = new CotizacionTableModel();

        inicializarControles();
        configurarControlesRestantes();
        inicializarTabla();
        configurarVentana();
        configurarActualizacionHorario();
    }

    private void configurarVentana() {
        setMinimumSize(new Dimension(1120, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(Math.max(getWidth(), 1240), Math.max(getHeight(), 780));
        setLocationRelativeTo(null);
    }

    private void inicializarControles() {
        rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        GridBagConstraints root = new GridBagConstraints();
        root.gridx = 0;
        root.weightx = 1;
        root.fill = GridBagConstraints.HORIZONTAL;
        root.insets = new Insets(0, 0, 14, 0);
        root.gridy = 0;
        rootPanel.add(ThemeMdp.createHeader(
            "Buscar lugares", onLogoutCallback, "Mis reservas", () -> onOpenReservations.run()
        ), root);
    }

    private void configurarControlesRestantes() {
        panelFiltros = new JPanel(new GridLayout(2, 7, 10, 8));
        ThemeMdp.stylePanel(panelFiltros);
        panelFiltros.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(10, 12, 12, 12)
        ));
        panelFiltros.add(etiquetaCampo("TIPO DE EVENTO"));
        panelFiltros.add(etiquetaCampo("INVITADOS"));
        panelFiltros.add(etiquetaCampo("FECHA"));
        panelFiltros.add(etiquetaCampo("HORA INICIO"));
        panelFiltros.add(etiquetaCampo("HORA FIN"));
        panelFiltros.add(etiquetaCampo("ZONA"));
        panelFiltros.add(etiquetaCampo(" "));

        cmbTipoEvento = new JComboBox<>();
        cmbZona = new JComboBox<>();
        spnInvitados = new JSpinner(new SpinnerNumberModel(30, 1, 2000, 1));
        txtFecha = new JTextField(LocalDate.now().plusDays(15).toString());
        txtHoraInicio = new JTextField("17:00");
        txtHoraFin = new JTextField("21:00");
        btnBuscar = new JButton("Buscar lugares");
        btnBuscar.setEnabled(false);
        JPanel guestField = new JPanel(new BorderLayout(0, 3));
        guestField.setOpaque(false);
        guestField.add(spnInvitados, BorderLayout.CENTER);
        lblErrorInvitados = new JLabel(" ");
        lblErrorInvitados.setForeground(new Color(0x9A, 0x3E, 0x3E));
        lblErrorInvitados.setFont(lblErrorInvitados.getFont().deriveFont(11f));
        guestField.add(lblErrorInvitados, BorderLayout.SOUTH);

        panelFiltros.add(cmbTipoEvento);
        panelFiltros.add(guestField);
        panelFiltros.add(txtFecha);
        panelFiltros.add(txtHoraInicio);
        panelFiltros.add(txtHoraFin);
        panelFiltros.add(cmbZona);
        panelFiltros.add(btnBuscar);

        lblHorario = new JLabel("Horario seleccionado: —", SwingConstants.LEFT);
        lblHorario.setForeground(new Color(0x4A, 0x5D, 0x68));
        lblHorario.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JPanel panelResultados = crearPanelResultados();
        panelAcciones = crearPanelAcciones();

        GridBagConstraints root = new GridBagConstraints();
        root.gridx = 0;
        root.weightx = 1;
        root.fill = GridBagConstraints.HORIZONTAL;
        root.insets = new Insets(0, 0, 10, 0);
        root.gridy = 1;
        rootPanel.add(panelFiltros, root);
        root.gridy = 2;
        root.insets = new Insets(0, 0, 8, 0);
        rootPanel.add(lblHorario, root);
        root.gridy = 3;
        root.weighty = 1;
        root.fill = GridBagConstraints.BOTH;
        root.insets = new Insets(0, 0, 12, 0);
        rootPanel.add(panelResultados, root);
        root.gridy = 4;
        root.weighty = 0;
        root.fill = GridBagConstraints.HORIZONTAL;
        root.insets = new Insets(0, 0, 0, 0);
        rootPanel.add(panelAcciones, root);

        cmbOrden.addActionListener(event -> {
            tblResultados.clearSelection();
            tableModel.ordenarPor((String) cmbOrden.getSelectedItem());
        });
        tblResultadosSelectionListener();
    }

    private JPanel crearPanelResultados() {
        JPanel results = new JPanel(new BorderLayout(0, 8));
        results.setBackground(Color.WHITE);
        results.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(12, 14, 8, 14)
        ));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("Disponibilidad y cotizaciones");
        title.setForeground(ThemeMdp.TEXT);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        toolbar.add(title, BorderLayout.WEST);
        JPanel order = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        order.setOpaque(false);
        order.add(new JLabel("Ordenar por"));
        cmbOrden = new JComboBox<>(new String[]{"Recomendados", "Menor tarifa", "Capacidad"});
        order.add(cmbOrden);
        toolbar.add(order, BorderLayout.EAST);
        results.add(toolbar, BorderLayout.NORTH);

        tblResultados = new JTable(tableModel);
        scrollResultados = new JScrollPane(tblResultados);
        scrollResultados.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        lblResultadosVacios = new JLabel(
            "No hay salones disponibles para esos criterios. Prueba otra fecha u horario, zona o cantidad de invitados.",
            SwingConstants.CENTER);
        lblResultadosVacios.setForeground(ThemeMdp.MUTED);
        scrollResultados.setViewportView(lblResultadosVacios);
        results.add(scrollResultados, BorderLayout.CENTER);

        JPanel attribution = new JPanel(new BorderLayout());
        attribution.setOpaque(false);
        lblAtribucion = new JLabel("Disponibilidad y tarifas informadas por los establecimientos");
        lblAtribucion.setForeground(ThemeMdp.MUTED);
        lblAtribucion.setFont(lblAtribucion.getFont().deriveFont(11f));
        lblUltimaConsulta = new JLabel("Última consulta: —");
        lblUltimaConsulta.setForeground(ThemeMdp.MUTED);
        lblUltimaConsulta.setFont(lblUltimaConsulta.getFont().deriveFont(11f));
        attribution.add(lblAtribucion, BorderLayout.WEST);
        attribution.add(lblUltimaConsulta, BorderLayout.EAST);
        results.add(attribution, BorderLayout.SOUTH);
        return results;
    }

    private JPanel crearPanelAcciones() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(new Color(0xED, 0xF2, 0xF3));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xCB, 0xD6, 0xDB)),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        JPanel summary = new JPanel(new BorderLayout(0, 4));
        summary.setOpaque(false);
        lblEstado = new JLabel("Seleccione criterios y busque salones disponibles.");
        lblEstado.setForeground(ThemeMdp.MUTED);
        lblResumenSeleccion = new JLabel("Seleccione un salón para ver su cotización estimada.");
        lblResumenSeleccion.setForeground(ThemeMdp.TEXT);
        lblResumenSeleccion.setFont(lblResumenSeleccion.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        progressBusqueda = new JProgressBar();
        progressBusqueda.setIndeterminate(true);
        progressBusqueda.setVisible(false);
        summary.add(lblEstado, BorderLayout.NORTH);
        summary.add(lblResumenSeleccion, BorderLayout.CENTER);
        summary.add(progressBusqueda, BorderLayout.SOUTH);
        bar.add(summary, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        btnLimpiar = new JButton("Limpiar filtros");
        btnContinuarReserva = new JButton("Reservar seleccionado");
        btnContinuarReserva.setEnabled(false);
        ThemeMdp.styleSecondaryButton(btnLimpiar);
        ThemeMdp.stylePrimaryButton(btnContinuarReserva);
        buttons.add(btnLimpiar);
        buttons.add(btnContinuarReserva);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private void inicializarTabla() {
        ThemeMdp.styleTable(tblResultados);
        tblResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblResultados.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer() {
            private final java.text.NumberFormat currency =
                java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number number ? currency.format(number) : "");
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };
        tblResultados.getColumnModel().getColumn(6).setCellRenderer(priceRenderer);
    }

    private void tblResultadosSelectionListener() {
        tblResultados.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) actualizarResumenSeleccion();
        });
    }

    private void configurarActualizacionHorario() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                actualizarNotaHorario();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                actualizarNotaHorario();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                actualizarNotaHorario();
            }
        };
        txtHoraInicio.getDocument().addDocumentListener(listener);
        txtHoraFin.getDocument().addDocumentListener(listener);
        actualizarNotaHorario();
    }

    private void actualizarNotaHorario() {
        try {
            LocalTime inicio = LocalTime.parse(txtHoraInicio.getText().trim());
            LocalTime fin = LocalTime.parse(txtHoraFin.getText().trim());
            boolean diaSiguiente = !fin.isAfter(inicio);
            lblHorario.setText("Horario seleccionado: " + inicio.format(DateTimeFormatter.ofPattern("HH:mm"))
                + "–" + fin.format(DateTimeFormatter.ofPattern("HH:mm"))
                + (diaSiguiente ? " (+1 día). Si la hora final es anterior, finaliza al día siguiente." : "."));
        } catch (DateTimeParseException exception) {
            lblHorario.setText("Ingrese las horas en formato de 24 horas HH:mm.");
        }
    }

    private static JLabel etiquetaCampo(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(new Color(0x16, 0x86, 0x7F));
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        return label;
    }

    public void setController(ClienteController controller) {
        btnBuscar.addActionListener(event -> controller.buscarDesdeVista());
        btnLimpiar.addActionListener(event -> controller.limpiar());
        btnContinuarReserva.addActionListener(event -> controller.confirmarReserva());
    }

    public void setOnOpenReservations(Runnable onOpenReservations) {
        this.onOpenReservations = java.util.Objects.requireNonNull(onOpenReservations, "onOpenReservations");
    }

    public void setCatalogos(List<TipoEvento> tipos, List<ZonaMdp> zonas) {
        cmbTipoEvento.removeAllItems();
        for (TipoEvento tipo : tipos) {
            cmbTipoEvento.addItem(new EventoOpcion(Math.toIntExact(tipo.getId()), tipo.getNombre()));
        }
        cmbZona.removeAllItems();
        cmbZona.addItem(new ZonaOpcion(null, "Todas las zonas"));
        for (ZonaMdp zona : zonas) {
            cmbZona.addItem(new ZonaOpcion(zona.getNombre(), zona.getNombre()));
        }
        btnBuscar.setEnabled(!tipos.isEmpty());
        if (tipos.isEmpty()) {
            mostrarEstado("No hay tipos de evento disponibles en la base de datos.");
        }
    }

    public int getTipoEventoSeleccionado() {
        EventoOpcion evento = (EventoOpcion) cmbTipoEvento.getSelectedItem();
        return evento == null ? 0 : evento.id();
    }

    public String getTipoEventoNombre() {
        EventoOpcion evento = (EventoOpcion) cmbTipoEvento.getSelectedItem();
        return evento == null ? "" : evento.nombre();
    }

    public int getInvitadosSeleccionados() {
        return (Integer) spnInvitados.getValue();
    }

    public String getFechaIngresada() {
        return txtFecha.getText().trim();
    }

    public String getHoraInicioIngresada() {
        return txtHoraInicio.getText().trim();
    }

    public String getHoraFinIngresada() {
        return txtHoraFin.getText().trim();
    }

    public String getZonaSeleccionada() {
        ZonaOpcion zona = (ZonaOpcion) cmbZona.getSelectedItem();
        return zona == null ? null : zona.nombreFiltro();
    }

    public CotizacionSalonDTO getCotizacionSeleccionada() {
        int filaSeleccionada = tblResultados.getSelectedRow();
        return filaSeleccionada < 0
            ? null
            : tableModel.getFila(tblResultados.convertRowIndexToModel(filaSeleccionada));
    }

    public void setBusquedaActiva(boolean activa) {
        progressBusqueda.setVisible(activa);
        btnBuscar.setEnabled(!activa && cmbTipoEvento.getSelectedItem() != null);
        btnLimpiar.setEnabled(!activa);
        btnContinuarReserva.setEnabled(!activa && getCotizacionSeleccionada() != null);
        cmbTipoEvento.setEnabled(!activa);
        cmbZona.setEnabled(!activa);
        spnInvitados.setEnabled(!activa);
        txtFecha.setEnabled(!activa);
        txtHoraInicio.setEnabled(!activa);
        txtHoraFin.setEnabled(!activa);
    }

    public void mostrarResultados(List<CotizacionSalonDTO> resultados) {
        progressBusqueda.setVisible(false);
        tableModel.setDatos(resultados);
        tblResultados.clearSelection();
        tableModel.ordenarPor((String) cmbOrden.getSelectedItem());
        if (resultados.isEmpty()) {
            scrollResultados.setViewportView(lblResultadosVacios);
            btnContinuarReserva.setEnabled(false);
            lblEstado.setText("No hay salones disponibles. Prueba con otra fecha u horario.");
        } else {
            scrollResultados.setViewportView(tblResultados);
            lblEstado.setText("Se encontraron " + resultados.size() + " opciones disponibles.");
        }
        lblUltimaConsulta.setText("Última consulta: "
            + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        actualizarResumenSeleccion();
    }

    public void mostrarSinDisponibilidadReciente() {
        tableModel.limpiar();
        tblResultados.clearSelection();
        scrollResultados.setViewportView(lblResultadosVacios);
        lblEstado.setText("El horario se ocupó mientras confirmabas. Cambia la fecha u horario y vuelve a buscar.");
        btnContinuarReserva.setEnabled(false);
        actualizarResumenSeleccion();
    }

    public void mostrarEstado(String estado) {
        lblEstado.setText(estado);
    }

    public void mostrarErrorInvitados(String mensaje) {
        lblErrorInvitados.setText(mensaje);
    }

    public void limpiarControles() {
        if (cmbTipoEvento.getItemCount() > 0) cmbTipoEvento.setSelectedIndex(0);
        spnInvitados.setValue(30);
        txtFecha.setText(LocalDate.now().plusDays(15).toString());
        txtHoraInicio.setText("17:00");
        txtHoraFin.setText("21:00");
        if (cmbZona.getItemCount() > 0) cmbZona.setSelectedIndex(0);
        tableModel.limpiar();
        scrollResultados.setViewportView(lblResultadosVacios);
        tblResultados.clearSelection();
        btnContinuarReserva.setEnabled(false);
        lblEstado.setText("Filtros restablecidos.");
        lblUltimaConsulta.setText("Última consulta: —");
    }

    public void mostrarMensaje(String mensaje, String titulo, int tipoJOptionPane) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipoJOptionPane);
    }

    public void abrirMisReservas() {
        onOpenReservations.run();
    }

    private void actualizarResumenSeleccion() {
        CotizacionSalonDTO seleccionada = getCotizacionSeleccionada();
        if (seleccionada == null) {
            lblResumenSeleccion.setText("Seleccione un salón para ver su cotización estimada.");
            btnContinuarReserva.setEnabled(false);
            return;
        }
        java.text.NumberFormat currency = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        lblResumenSeleccion.setText("Seleccionado: " + seleccionada.salon() + "  ·  Cotización estimada: "
            + currency.format(seleccionada.precioTotalEstimado()));
        btnContinuarReserva.setEnabled(true);
    }

    private record EventoOpcion(int id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private record ZonaOpcion(String nombreFiltro, String nombreVisible) {
        @Override
        public String toString() {
            return nombreVisible;
        }
    }
}
