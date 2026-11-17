package com.mdp.eventos.view.proveedor;

import com.mdp.eventos.controller.ProveedorController;
import com.mdp.eventos.dto.ReservaProveedorDTO;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.view.components.ProveedorReservaTableModel;
import com.mdp.eventos.view.components.StatusBadgeCellRenderer;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class GestionReservasProveedorDialog extends JDialog {
    private final ProveedorController controller;
    private final ProveedorReservaTableModel tableModel = new ProveedorReservaTableModel();
    private final JTable tabla = new JTable(tableModel);
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnConfirmar = new JButton("Confirmar solicitud");
    private final JButton btnRechazar = new JButton("Rechazar solicitud");
    private final JButton btnCancelar = new JButton("Cancelar confirmada");
    private final JLabel lblEstado = new JLabel("Cargando solicitudes...");

    GestionReservasProveedorDialog(Window owner, ProveedorController controller) {
        super(owner, "Solicitudes de reserva", ModalityType.MODELESS);
        this.controller = Objects.requireNonNull(controller, "controller");
        construir();
        cargar();
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        ThemeMdp.styleWindow(root);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel title = new JLabel("Solicitudes de tus establecimientos");
        title.setForeground(ThemeMdp.TEXT);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        ThemeMdp.styleTable(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(9).setCellRenderer(new StatusBadgeCellRenderer());
        tabla.getColumnModel().getColumn(10).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(8).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private final NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number number ? currency.format(number) : "");
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        tabla.getColumnModel().getColumn(2).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(180);
        root.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        lblEstado.setForeground(ThemeMdp.MUTED);
        footer.add(lblEstado, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        ThemeMdp.styleSecondaryButton(btnActualizar);
        ThemeMdp.stylePrimaryButton(btnConfirmar);
        ThemeMdp.styleDangerButton(btnRechazar);
        ThemeMdp.styleDangerButton(btnCancelar);
        btnConfirmar.setEnabled(false);
        btnRechazar.setEnabled(false);
        btnCancelar.setEnabled(false);
        actions.add(btnActualizar);
        actions.add(btnConfirmar);
        actions.add(btnRechazar);
        actions.add(btnCancelar);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        tabla.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) actualizarAcciones();
        });
        btnActualizar.addActionListener(event -> cargar());
        btnConfirmar.addActionListener(event -> cambiarEstado(EstadoReserva.CONFIRMADA));
        btnRechazar.addActionListener(event -> cambiarEstado(EstadoReserva.RECHAZADA));
        btnCancelar.addActionListener(event -> cancelarConfirmada());
        setContentPane(root);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 600));
        setSize(1320, 700);
        setLocationRelativeTo(getOwner());
    }

    private void cargar() {
        setCargando(true);
        lblEstado.setText("Consultando solicitudes de tus establecimientos...");
        controller.cargarReservasProveedor(this::mostrarReservas, this::mostrarError);
    }

    private void mostrarReservas(List<ReservaProveedorDTO> reservas) {
        setCargando(false);
        tableModel.setReservas(reservas);
        lblEstado.setText(reservas.isEmpty()
            ? "No hay solicitudes para tus establecimientos."
            : reservas.size() + " reservas; las pendientes requieren tu respuesta.");
        actualizarAcciones();
    }

    private void cambiarEstado(EstadoReserva estado) {
        ReservaProveedorDTO seleccionada = getSeleccionada();
        if (seleccionada == null || !"PENDIENTE".equals(seleccionada.estado())) return;
        String accion = estado == EstadoReserva.CONFIRMADA ? "confirmar" : "rechazar";
        String motivo = estado == EstadoReserva.RECHAZADA ? pedirMotivo("Motivo del rechazo") : null;
        if (estado == EstadoReserva.RECHAZADA && motivo == null) return;
        int respuesta = JOptionPane.showConfirmDialog(
            this,
            "¿Quieres " + accion + " la solicitud " + seleccionada.codigo()
                + " para " + seleccionada.cliente() + "?",
            "Actualizar solicitud",
            JOptionPane.YES_NO_OPTION,
            estado == EstadoReserva.CONFIRMADA ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE
        );
        if (respuesta != JOptionPane.YES_OPTION) return;
        setCargando(true);
        lblEstado.setText("Actualizando estado en MySQL...");
        controller.actualizarEstadoReserva(
            seleccionada.reservaId(),
            estado,
            motivo,
            () -> {
                JOptionPane.showMessageDialog(
                    this,
                    "La solicitud " + seleccionada.codigo() + " quedó "
                        + (estado == EstadoReserva.CONFIRMADA ? "confirmada." : "rechazada.")
                );
                cargar();
            },
            this::mostrarError
        );
    }

    private void cancelarConfirmada() {
        ReservaProveedorDTO seleccionada = getSeleccionada();
        if (seleccionada == null || !"CONFIRMADA".equals(seleccionada.estado())) return;
        String motivo = pedirMotivo("Motivo de cancelación");
        if (motivo == null) return;
        setCargando(true);
        controller.cancelarReserva(
            seleccionada.reservaId(),
            motivo,
            () -> {
                JOptionPane.showMessageDialog(this, "La reserva quedó cancelada con el motivo registrado.");
                cargar();
            },
            this::mostrarError
        );
    }

    private String pedirMotivo(String titulo) {
        JTextArea area = new JTextArea(4, 36);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        int answer = JOptionPane.showConfirmDialog(
            this, new JScrollPane(area), titulo + " (obligatorio)", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (answer != JOptionPane.OK_OPTION) return null;
        String reason = area.getText().trim();
        if (reason.isEmpty() || reason.length() > 500) {
            JOptionPane.showMessageDialog(this, "Ingrese un motivo de hasta 500 caracteres.");
            return null;
        }
        return reason;
    }

    private void actualizarAcciones() {
        boolean pendiente = !estaCargando()
            && getSeleccionada() != null
            && "PENDIENTE".equals(getSeleccionada().estado());
        btnConfirmar.setEnabled(pendiente);
        btnRechazar.setEnabled(pendiente);
        ReservaProveedorDTO selected = getSeleccionada();
        btnCancelar.setEnabled(!estaCargando() && selected != null
            && "CONFIRMADA".equals(selected.estado())
            && java.time.LocalDateTime.of(selected.fecha(), selected.horaInicio()).isAfter(java.time.LocalDateTime.now()));
    }

    private ReservaProveedorDTO getSeleccionada() {
        int selected = tabla.getSelectedRow();
        return selected < 0 ? null : tableModel.getReserva(tabla.convertRowIndexToModel(selected));
    }

    private void setCargando(boolean cargando) {
        btnActualizar.setEnabled(!cargando);
        if (cargando) {
            btnConfirmar.setEnabled(false);
            btnRechazar.setEnabled(false);
            btnCancelar.setEnabled(false);
        } else {
            actualizarAcciones();
        }
    }

    private boolean estaCargando() {
        return !btnActualizar.isEnabled();
    }

    private void mostrarError(String mensaje) {
        setCargando(false);
        lblEstado.setText("No se pudo completar la operación.");
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
