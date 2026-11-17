package com.mdp.eventos.view.cliente;

import com.mdp.eventos.controller.ClienteReservasController;
import com.mdp.eventos.dto.ResumenReservaDTO;
import com.mdp.eventos.view.components.ReservaTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MisReservasPanel extends JPanel {
    private JPanel rootPanel = new JPanel(new GridBagLayout());
    private final ReservaTableModel tableModel = new ReservaTableModel();
    private JTextField txtDesde;
    private JTextField txtHasta;
    private JComboBox<String> cmbEstado;
    private JTable tblReservas;
    private JLabel lblEstado;
    private JButton btnAplicar;
    private JButton btnCancelar;
    private JButton btnDetalle;
    private ClienteReservasController controller;
    private final Runnable onBack;
    private final Runnable onLogout;

    public MisReservasPanel(Runnable onBack, Runnable onLogout) {
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
        setBackground(ThemeMdp.BACKGROUND);
        ThemeMdp.styleWindow(rootPanel);
        construir();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
    }

    private void construir() {
        rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        c.gridy = 0;
        rootPanel.add(ThemeMdp.createHeader("Mis reservas", onLogout, "Volver al buscador", onBack), c);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filters.setBackground(Color.WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        filters.add(fieldLabel("ESTADO"));
        cmbEstado = new JComboBox<>(new String[]{
            "Todos", "Pendiente", "Confirmada", "Rechazada", "Cancelada", "Vencida", "Finalizada"
        });
        filters.add(cmbEstado);
        filters.add(fieldLabel("DESDE · AAAA-MM-DD"));
        txtDesde = new JTextField(10);
        filters.add(txtDesde);
        filters.add(fieldLabel("HASTA · AAAA-MM-DD"));
        txtHasta = new JTextField(10);
        filters.add(txtHasta);
        btnAplicar = new JButton("Aplicar filtros");
        ThemeMdp.stylePrimaryButton(btnAplicar);
        filters.add(btnAplicar);
        c.gridy = 1;
        rootPanel.add(filters, c);

        tblReservas = new JTable(tableModel);
        ThemeMdp.styleTable(tblReservas);
        tblReservas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservas.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            private final java.text.NumberFormat currency =
                java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number number ? currency.format(number) : "");
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        tblReservas.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeCellRenderer());
        JScrollPane scroll = new JScrollPane(tblReservas);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        c.gridy = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 12, 0);
        rootPanel.add(scroll, c);

        JPanel actionBar = new JPanel(new BorderLayout(12, 0));
        actionBar.setBackground(new Color(0xED, 0xF2, 0xF3));
        actionBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xCB, 0xD6, 0xDB)),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        lblEstado = new JLabel("Cargando sus reservas desde MySQL...");
        actionBar.add(lblEstado, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnDetalle = new JButton("Ver detalle");
        btnCancelar = new JButton("Cancelar reserva");
        ThemeMdp.styleSecondaryButton(btnDetalle);
        ThemeMdp.styleDangerButton(btnCancelar);
        btnDetalle.setEnabled(false);
        btnCancelar.setEnabled(false);
        actions.add(btnDetalle);
        actions.add(btnCancelar);
        actionBar.add(actions, BorderLayout.EAST);
        c.gridy = 3;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        rootPanel.add(actionBar, c);

        tblReservas.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) actualizarAcciones();
        });
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x16, 0x86, 0x7F));
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        return label;
    }

    public void setController(ClienteReservasController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
        btnAplicar.addActionListener(event -> controller.cargar());
        btnCancelar.addActionListener(event -> solicitarCancelacion());
        btnDetalle.addActionListener(event -> mostrarDetalleSeleccionada());
    }

    private void solicitarCancelacion() {
        ResumenReservaDTO reserva = getReservaSeleccionada();
        if (reserva == null || !puedeCancelar(reserva)) return;
        int answer = JOptionPane.showConfirmDialog(
            this,
            "¿Cancelar la reserva " + reserva.codigoReserva()
                + "?\nPuedes cancelarla mientras el evento todavía no haya comenzado.",
            "Cancelar reserva",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION) controller.cancelar(reserva.reservaId());
    }

    private void mostrarDetalleSeleccionada() {
        ResumenReservaDTO reserva = getReservaSeleccionada();
        if (reserva == null) return;
        String horario = reserva.horaInicio() + "–" + reserva.horaFin()
            + (reserva.horaFin().isBefore(reserva.horaInicio()) ? " (+1 día)" : "");
        String detail = "Código: " + reserva.codigoReserva()
            + "\nEstado: " + reserva.estado()
            + (reserva.motivoEstado() == null || reserva.motivoEstado().isBlank()
                ? "" : "\nMotivo: " + reserva.motivoEstado())
            + "\nEstablecimiento: " + reserva.establecimiento()
            + "\nSalón: " + reserva.salon()
            + "\nFecha: " + reserva.fechaEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            + "\nHorario: " + horario
            + "\nInvitados: " + reserva.cantidadInvitados()
            + "\nTotal: " + ReservaTableModel.formatear(reserva.montoTotal());
        JOptionPane.showMessageDialog(this, detail, "Detalle de reserva", JOptionPane.INFORMATION_MESSAGE);
    }

    private void actualizarAcciones() {
        ResumenReservaDTO reserva = getReservaSeleccionada();
        btnDetalle.setEnabled(reserva != null);
        btnCancelar.setEnabled(reserva != null && puedeCancelar(reserva));
    }

    private boolean puedeCancelar(ResumenReservaDTO reserva) {
        boolean estadoCancelable = "PENDIENTE".equalsIgnoreCase(reserva.estado())
            || "CONFIRMADA".equalsIgnoreCase(reserva.estado());
        return estadoCancelable
            && LocalDateTime.of(reserva.fechaEvento(), reserva.horaInicio()).isAfter(LocalDateTime.now());
    }

    public String getEstadoSeleccionado() {
        return (String) cmbEstado.getSelectedItem();
    }

    public String getDesdeIngresado() {
        return txtDesde.getText().trim();
    }

    public String getHastaIngresado() {
        return txtHasta.getText().trim();
    }

    public ResumenReservaDTO getReservaSeleccionada() {
        int selected = tblReservas.getSelectedRow();
        return selected < 0 ? null : tableModel.getReserva(tblReservas.convertRowIndexToModel(selected));
    }

    public void setCargando(boolean cargando) {
        btnAplicar.setEnabled(!cargando);
        btnCancelar.setEnabled(!cargando && btnCancelar.isEnabled());
        btnDetalle.setEnabled(!cargando && getReservaSeleccionada() != null);
        txtDesde.setEnabled(!cargando);
        txtHasta.setEnabled(!cargando);
        cmbEstado.setEnabled(!cargando);
        lblEstado.setText(cargando ? "Consultando reservas en MySQL..." : lblEstado.getText());
    }

    public void mostrarReservas(List<ResumenReservaDTO> reservas) {
        tableModel.setReservas(reservas);
        tblReservas.clearSelection();
        actualizarAcciones();
        mostrarEstado(reservas.isEmpty()
            ? "No se encontraron reservas con estos filtros."
            : "Se encontraron " + reservas.size() + " reservas de su cuenta.");
    }

    public void mostrarEstado(String estado) {
        lblEstado.setForeground(ThemeMdp.MUTED);
        lblEstado.setText(estado);
    }

    public void mostrarError(String error) {
        lblEstado.setForeground(new Color(0x9A, 0x3E, 0x3E));
        lblEstado.setText(error);
    }
}
