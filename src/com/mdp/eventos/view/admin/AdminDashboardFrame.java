package com.mdp.eventos.view.admin;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.controller.AdminController;
import com.mdp.eventos.dto.PanelAdminDTO;
import com.mdp.eventos.dto.ReservaAdminDTO;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.view.components.AdminReservaTableModel;
import com.mdp.eventos.view.components.AdminUsuarioTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AdminDashboardFrame extends JFrame {
    private final JLabel lblEstado = new JLabel("Cargando información desde MySQL...");
    private final JLabel lblUsuarios = new JLabel("—");
    private final JLabel lblEstablecimientos = new JLabel("—");
    private final JLabel lblPendientes = new JLabel("—");
    private final JLabel lblUltimaReserva = new JLabel("—");
    private final AdminUsuarioTableModel usuariosModel = new AdminUsuarioTableModel();
    private final AdminReservaTableModel reservasModel = new AdminReservaTableModel();
    private final JTable tblUsuarios = new JTable(usuariosModel);
    private final JTable tblReservas = new JTable(reservasModel);
    private final JTextField txtBuscarUsuario = new JTextField(18);
    private final JComboBox<String> cmbRol = new JComboBox<>(new String[]{
        "Todos los roles", "Administrador", "Proveedor", "Cliente"
    });
    private final JComboBox<String> cmbEstadoUsuario = new JComboBox<>(new String[]{
        "Todos los estados", "Activo", "Inactivo"
    });
    private final JTextField txtBuscarReserva = new JTextField(20);
    private final JComboBox<String> cmbEstadoReserva = new JComboBox<>(new String[]{
        "Todos los estados", "Pendiente", "Confirmada", "Rechazada", "Cancelada", "Vencida", "Finalizada"
    });
    private final JButton btnReservaAnterior = new JButton("Anterior");
    private final JButton btnReservaSiguiente = new JButton("Siguiente");
    private final JButton btnCancelarReserva = new JButton("Cancelar reserva (motivo)");
    private final JLabel lblPaginaReservas = new JLabel("Página 1");
    private int paginaReservas;
    private long totalReservas;
    private static final int TAMANIO_PAGINA_RESERVAS = 50;
    private final JButton btnCambiarEstado = new JButton("Desactivar cuenta");
    private final JButton btnActualizar = new JButton("Actualizar datos");
    private final TableRowSorter<AdminUsuarioTableModel> sorterUsuarios =
        new TableRowSorter<>(usuariosModel);
    private final TableRowSorter<AdminReservaTableModel> sorterReservas =
        new TableRowSorter<>(reservasModel);
    private final javax.swing.Timer reservaFilterTimer = new javax.swing.Timer(300, event -> {
        paginaReservas = 0;
        cargarDatos();
    });
    private AdminController controller;
    private final CatalogosAdminPanel catalogosPanel = new CatalogosAdminPanel();
    private long secuenciaCarga;

    public AdminDashboardFrame(Runnable onLogout) {
        super("Eventos MDP | Panel de administración");
        Objects.requireNonNull(onLogout, "onLogout");

        JPanel root = new JPanel(new BorderLayout(0, 14));
        ThemeMdp.styleWindow(root);
        root.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        root.add(ThemeMdp.createHeader("Panel de administración", onLogout), BorderLayout.NORTH);
        root.add(crearContenido(), BorderLayout.CENTER);

        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1320, 820);
        setLocationRelativeTo(null);
    }

    public void setController(AdminController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
        btnActualizar.addActionListener(event -> cargarDatos());
        btnCambiarEstado.addActionListener(event -> cambiarEstadoUsuario());
        txtBuscarUsuario.getDocument().addDocumentListener(new DocumentChangeListener(this::filtrarUsuarios));
        txtBuscarReserva.getDocument().addDocumentListener(new DocumentChangeListener(this::filtrarReservas));
        cmbRol.addActionListener(event -> filtrarUsuarios());
        cmbEstadoUsuario.addActionListener(event -> filtrarUsuarios());
        cmbEstadoReserva.addActionListener(event -> filtrarReservas());
        tblUsuarios.getSelectionModel().addListSelectionListener(event -> actualizarBotonEstadoUsuario());
        tblReservas.getSelectionModel().addListSelectionListener(event -> actualizarBotonCancelarReserva());
        catalogosPanel.setController(controller);
        cargarDatos();
    }

    private JPanel crearContenido() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        JPanel intro = new JPanel(new BorderLayout(12, 0));
        intro.setOpaque(false);
        JLabel title = new JLabel("Administración del sistema");
        title.setForeground(ThemeMdp.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        intro.add(title, BorderLayout.WEST);
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        updatePanel.setOpaque(false);
        lblEstado.setForeground(ThemeMdp.MUTED);
        updatePanel.add(lblEstado);
        ThemeMdp.styleSecondaryButton(btnActualizar);
        updatePanel.add(btnActualizar);
        intro.add(updatePanel, BorderLayout.EAST);
        content.add(intro, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resumen", crearPestanaResumen());
        tabs.addTab("Usuarios", crearPestanaUsuarios());
        tabs.addTab("Reservas", crearPestanaReservas());
        tabs.addTab("Catálogos", catalogosPanel);
        content.add(tabs, BorderLayout.CENTER);
        return content;
    }

    private JPanel crearPestanaResumen() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
        kpis.setOpaque(false);
        kpis.add(crearTarjeta("USUARIOS REGISTRADOS", lblUsuarios));
        kpis.add(crearTarjeta("ESTABLECIMIENTOS ACTIVOS", lblEstablecimientos));
        kpis.add(crearTarjeta("RESERVAS PENDIENTES", lblPendientes));
        kpis.add(crearTarjeta("ÚLTIMA RESERVA", lblUltimaReserva));
        panel.add(kpis, BorderLayout.NORTH);

        JPanel information = new JPanel(new BorderLayout(0, 12));
        information.setBackground(Color.WHITE);
        information.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(20, 22, 20, 22)
        ));
        JLabel heading = new JLabel("Herramientas disponibles");
        heading.setForeground(ThemeMdp.TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
        information.add(heading, BorderLayout.NORTH);
        JLabel description = new JLabel("<html><div style='width: 700px; line-height: 1.5;'>"
            + "Desde <b>Usuarios</b> puedes consultar cuentas por rol o estado y activar/desactivar su acceso. "
            + "Desde <b>Reservas</b> puedes revisar las solicitudes y filtrar por estado. "
            + "Los indicadores y las listas se consultan directamente en MySQL. "
            + "La cuenta conectada y el último administrador activo están protegidos.</div></html>");
        description.setForeground(ThemeMdp.MUTED);
        information.add(description, BorderLayout.CENTER);
        panel.add(information, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjeta(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        JLabel heading = new JLabel(title);
        heading.setForeground(ThemeMdp.MUTED);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
        value.setForeground(ThemeMdp.TEXT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 24f));
        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearPestanaUsuarios() {
        JPanel panel = crearPanelTabla();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        filters.add(new JLabel("Buscar"));
        filters.add(txtBuscarUsuario);
        filters.add(new JLabel("Rol"));
        filters.add(cmbRol);
        filters.add(new JLabel("Estado"));
        filters.add(cmbEstadoUsuario);
        panel.add(filters, BorderLayout.NORTH);

        configurarTabla(tblUsuarios);
        tblUsuarios.setRowSorter(sorterUsuarios);
        tblUsuarios.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeCellRenderer());
        JScrollPane scroll = new JScrollPane(tblUsuarios);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JLabel note = new JLabel("No se muestran contraseñas. Los cambios de acceso se aplican en MySQL.");
        note.setForeground(ThemeMdp.MUTED);
        actions.add(note, BorderLayout.CENTER);
        ThemeMdp.styleDangerButton(btnCambiarEstado);
        btnCambiarEstado.setEnabled(false);
        actions.add(btnCambiarEstado, BorderLayout.EAST);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPestanaReservas() {
        JPanel panel = crearPanelTabla();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        filters.add(new JLabel("Buscar"));
        filters.add(txtBuscarReserva);
        filters.add(new JLabel("Estado"));
        filters.add(cmbEstadoReserva);
        panel.add(filters, BorderLayout.NORTH);

        configurarTabla(tblReservas);
        tblReservas.setRowSorter(sorterReservas);
        tblReservas.getColumnModel().getColumn(9).setCellRenderer(new StatusBadgeCellRenderer());
        tblReservas.getColumnModel().getColumn(10).setPreferredWidth(220);
        tblReservas.getColumnModel().getColumn(2).setPreferredWidth(180);
        tblReservas.getColumnModel().getColumn(3).setPreferredWidth(180);
        tblReservas.getColumnModel().getColumn(8).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private final java.text.NumberFormat currency =
                java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number number ? currency.format(number) : "");
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        JScrollPane scroll = new JScrollPane(tblReservas);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel(
            "El proveedor confirma o rechaza; el administrador solo cancela excepcionalmente con motivo."
        );
        note.setForeground(ThemeMdp.MUTED);
        footer.add(note, BorderLayout.WEST);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        ThemeMdp.styleSecondaryButton(btnReservaAnterior);
        ThemeMdp.styleSecondaryButton(btnReservaSiguiente);
        ThemeMdp.styleDangerButton(btnCancelarReserva);
        btnCancelarReserva.setEnabled(false);
        btnReservaAnterior.addActionListener(event -> {
            if (paginaReservas > 0) {
                paginaReservas--;
                cargarDatos();
            }
        });
        btnReservaSiguiente.addActionListener(event -> {
            if ((long) (paginaReservas + 1) * TAMANIO_PAGINA_RESERVAS < totalReservas) {
                paginaReservas++;
                cargarDatos();
            }
        });
        btnCancelarReserva.addActionListener(event -> cancelarReservaSeleccionada());
        paging.add(btnCancelarReserva);
        paging.add(btnReservaAnterior);
        paging.add(lblPaginaReservas);
        paging.add(btnReservaSiguiente);
        footer.add(paging, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    private static void configurarTabla(JTable table) {
        ThemeMdp.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    private void cargarDatos() {
        if (controller == null) return;
        long secuencia = ++secuenciaCarga;
        btnActualizar.setEnabled(false);
        lblEstado.setText("Actualizando desde MySQL…");
        String selectedState = (String) cmbEstadoReserva.getSelectedItem();
        String state = switch (selectedState == null ? "" : selectedState) {
            case "Pendiente" -> "PENDIENTE";
            case "Confirmada" -> "CONFIRMADA";
            case "Rechazada" -> "RECHAZADA";
            case "Cancelada" -> "CANCELADA";
            case "Vencida" -> "VENCIDA";
            case "Finalizada" -> "FINALIZADA";
            default -> null;
        };
        controller.cargar(
            paginaReservas, TAMANIO_PAGINA_RESERVAS, txtBuscarReserva.getText().trim(), state,
            data -> {
                if (secuencia == secuenciaCarga) mostrarDatos(data);
            },
            error -> {
            if (secuencia != secuenciaCarga) return;
            btnActualizar.setEnabled(true);
            lblEstado.setText("No se pudo actualizar");
            mostrarError("No se pudo cargar el panel", error);
            }
        );
    }

    private void mostrarDatos(PanelAdminDTO data) {
        usuariosModel.setUsuarios(data.usuarios());
        reservasModel.setReservas(data.reservas());
        totalReservas = data.totalReservas();
        lblUsuarios.setText(Long.toString(data.resumen().usuariosRegistrados()));
        lblEstablecimientos.setText(Long.toString(data.resumen().establecimientosActivos()));
        lblPendientes.setText(Long.toString(data.resumen().reservasPendientes()));
        lblUltimaReserva.setFont(lblUltimaReserva.getFont().deriveFont(Font.BOLD, 16f));
        lblUltimaReserva.setText(data.resumen().ultimaReserva() == null
            ? "Sin reservas"
            : data.resumen().ultimaReserva().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")));
        lblEstado.setText("Datos actualizados desde MySQL");
        btnActualizar.setEnabled(true);
        filtrarUsuarios();
        sorterReservas.setRowFilter(null);
        actualizarBotonEstadoUsuario();
        actualizarPaginacion();
        actualizarBotonCancelarReserva();
    }

    private void filtrarUsuarios() {
        if (tblUsuarios.getRowSorter() == null) return;
        String text = txtBuscarUsuario.getText().trim().toLowerCase(Locale.ROOT);
        String selectedRole = (String) cmbRol.getSelectedItem();
        String role = switch (selectedRole == null ? "" : selectedRole) {
            case "Administrador" -> Rol.ROLE_ADMIN;
            case "Proveedor" -> Rol.ROLE_PROVEEDOR;
            case "Cliente" -> Rol.ROLE_CLIENTE;
            default -> null;
        };
        String status = switch ((String) cmbEstadoUsuario.getSelectedItem()) {
            case "Activo" -> "Activo";
            case "Inactivo" -> "Inactivo";
            default -> null;
        };
        sorterUsuarios.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends AdminUsuarioTableModel, ? extends Integer> entry) {
                boolean matchesText = text.isEmpty()
                    || entry.getStringValue(1).toLowerCase(Locale.ROOT).contains(text)
                    || entry.getStringValue(2).toLowerCase(Locale.ROOT).contains(text)
                    || entry.getStringValue(4).toLowerCase(Locale.ROOT).contains(text);
                boolean matchesRole = role == null || role.equals(entry.getStringValue(3));
                boolean matchesStatus = status == null || status.equals(entry.getStringValue(5));
                return matchesText && matchesRole && matchesStatus;
            }
        });
    }

    private void filtrarReservas() {
        if (tblReservas.getRowSorter() == null) return;
        reservaFilterTimer.restart();
    }

    private void actualizarPaginacion() {
        long paginas = Math.max(1, (totalReservas + TAMANIO_PAGINA_RESERVAS - 1) / TAMANIO_PAGINA_RESERVAS);
        if (paginaReservas >= paginas) {
            paginaReservas = (int) paginas - 1;
            cargarDatos();
            return;
        }
        lblPaginaReservas.setText("Página " + (paginaReservas + 1) + " de " + paginas
            + " · " + totalReservas + " reservas");
        btnReservaAnterior.setEnabled(paginaReservas > 0);
        btnReservaSiguiente.setEnabled((long) (paginaReservas + 1) * TAMANIO_PAGINA_RESERVAS < totalReservas);
    }

    private void actualizarBotonCancelarReserva() {
        int selected = tblReservas.getSelectedRow();
        if (selected < 0) {
            btnCancelarReserva.setEnabled(false);
            return;
        }
        ReservaAdminDTO reserva = reservasModel.getReserva(tblReservas.convertRowIndexToModel(selected));
        boolean cancelable = ("PENDIENTE".equals(reserva.estado()) || "CONFIRMADA".equals(reserva.estado()))
            && java.time.LocalDateTime.of(reserva.fecha(), reserva.horaInicio()).isAfter(java.time.LocalDateTime.now());
        btnCancelarReserva.setEnabled(cancelable);
    }

    private void cancelarReservaSeleccionada() {
        int selected = tblReservas.getSelectedRow();
        if (selected < 0) return;
        ReservaAdminDTO reserva = reservasModel.getReserva(tblReservas.convertRowIndexToModel(selected));
        JTextArea reason = new JTextArea(4, 36);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        int answer = JOptionPane.showConfirmDialog(
            this, new JScrollPane(reason), "Motivo obligatorio de cancelación",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.OK_OPTION) return;
        String motivo = reason.getText().trim();
        if (motivo.isEmpty() || motivo.length() > 500) {
            mostrarError("Motivo inválido", "El motivo es obligatorio y admite hasta 500 caracteres.");
            return;
        }
        btnCancelarReserva.setEnabled(false);
        controller.cancelarReserva(reserva.reservaId(), motivo, this::cargarDatos, error -> {
            mostrarError("No se pudo cancelar la reserva", error);
            actualizarBotonCancelarReserva();
        });
    }

    private void actualizarBotonEstadoUsuario() {
        int selected = tblUsuarios.getSelectedRow();
        if (selected < 0) {
            btnCambiarEstado.setEnabled(false);
            return;
        }
        Usuario user = usuariosModel.getUsuario(tblUsuarios.convertRowIndexToModel(selected));
        long sessionUserId = SesionActual.getUsuario().map(Usuario::getId).orElse(-1L);
        boolean canChange = user.getId() != sessionUserId;
        btnCambiarEstado.setEnabled(canChange);
        btnCambiarEstado.setText(user.isActivo() ? "Desactivar cuenta" : "Activar cuenta");
        btnCambiarEstado.setToolTipText(canChange ? null : "No puedes cambiar el estado de tu propia cuenta.");
    }

    private void cambiarEstadoUsuario() {
        int selected = tblUsuarios.getSelectedRow();
        if (selected < 0) return;
        Usuario user = usuariosModel.getUsuario(tblUsuarios.convertRowIndexToModel(selected));
        boolean newState = !user.isActivo();
        String action = newState ? "activar" : "desactivar";
        String consequence = !newState && user.getRol() != null
            && com.mdp.eventos.model.Rol.ROLE_PROVEEDOR.equals(user.getRol().getNombre())
            ? "\n\nSus reservas pendientes se cancelarán con auditoría y las confirmadas futuras quedarán marcadas para revisión."
            : "";
        int answer = JOptionPane.showConfirmDialog(
            this,
            "¿Confirmas " + action + " la cuenta de " + user.getNombreCompleto() + " (" + user.getEmail() + ")?"
                + consequence,
            "Confirmar cambio de acceso",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) return;

        btnCambiarEstado.setEnabled(false);
        btnActualizar.setEnabled(false);
        controller.actualizarEstadoUsuario(user.getId(), newState, () -> {
            lblEstado.setText("Estado de cuenta actualizado");
            cargarDatos();
        }, error -> {
            btnActualizar.setEnabled(true);
            actualizarBotonEstadoUsuario();
            mostrarError("No se pudo cambiar el estado de la cuenta", error);
        });
    }

    private void mostrarError(String titulo, String detalle) {
        JLabel message = new JLabel("<html><div style='width: 430px;'>"
            + escaparHtml(detalle) + "</div></html>");
        JOptionPane.showMessageDialog(this, message, titulo, JOptionPane.ERROR_MESSAGE);
    }

    private static String escaparHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class DocumentChangeListener implements javax.swing.event.DocumentListener {
        private final Runnable action;

        private DocumentChangeListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent event) {
            action.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent event) {
            action.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent event) {
            action.run();
        }
    }
}
