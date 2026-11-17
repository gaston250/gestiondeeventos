package com.mdp.eventos.view.cliente;

import com.mdp.eventos.dto.CotizacionSalonDTO;
import com.mdp.eventos.model.ServicioAdicional;
import com.mdp.eventos.service.HorarioEvento;
import com.mdp.eventos.service.SalonInfoApiService;
import com.mdp.eventos.view.components.ServicioAdicionalTableModel;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.Cursor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.net.URI;

public final class DetalleCotizacionDialog extends JDialog {
    private JPanel rootPanel = new JPanel(new BorderLayout(0, 14));
    private final ServicioAdicionalTableModel serviceModel;
    private final JLabel rentalValue = new JLabel();
    private final JLabel guestValue = new JLabel();
    private final JLabel servicesValue = new JLabel();
    private final JLabel totalValue = new JLabel();
    private boolean confirmed;
    private BigDecimal totalEstimado = BigDecimal.ZERO;
    private final JLabel lblApiInfo = new JLabel("Mapa y pronóstico: consultando fuentes externas opcionales...");
    private URI mapaUri;

    public DetalleCotizacionDialog(
        Window owner,
        CotizacionSalonDTO cotizacion,
        LocalDate fecha,
        String tipoEvento,
        List<ServicioAdicional> servicios
    ) {
        super(owner, "Detalle de cotización", Dialog.ModalityType.APPLICATION_MODAL);
        Objects.requireNonNull(cotizacion, "cotizacion");
        Objects.requireNonNull(fecha, "fecha");
        serviceModel = new ServicioAdicionalTableModel(servicios, cotizacion.invitados());
        construir(cotizacion, fecha, tipoEvento);
        setMinimumSize(new Dimension(900, 650));
        setSize(1020, 740);
        setLocationRelativeTo(owner);
    }

    private void construir(CotizacionSalonDTO cotizacion, LocalDate fecha, String tipoEvento) {
        rootPanel.setBackground(ThemeMdp.BACKGROUND);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel title = new JLabel("Revisa los detalles antes de solicitar la reserva");
        title.setForeground(ThemeMdp.TEXT);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        rootPanel.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setOpaque(false);
        cards.add(card("DATOS DEL EVENTO", datosEvento(cotizacion, fecha, tipoEvento)));
        cards.add(cardServicios());
        cards.add(cardDesglose(cotizacion));
        cards.add(cardConfirmacion());
        rootPanel.add(cards, BorderLayout.CENTER);
        setContentPane(rootPanel);

        serviceModel.addTableModelListener(event -> {
            if (event.getType() == TableModelEvent.UPDATE) actualizarTotal(cotizacion);
        });
        actualizarTotal(cotizacion);
        consultarInformacionOpcional(cotizacion.direccion(), fecha);
    }

    private JPanel datosEvento(CotizacionSalonDTO quote, LocalDate date, String eventName) {
        JPanel details = new JPanel(new GridLayout(0, 1, 0, 7));
        details.setOpaque(false);
        addDetail(details, "Establecimiento", quote.establecimiento());
        addDetail(details, "Salón", quote.salon());
        addDetail(details, "Evento", eventName);
        addDetail(details, "Fecha", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String overnight = HorarioEvento.cruzaMedianoche(quote.horaInicio(), quote.horaFin()) ? " (+1 día)" : "";
        addDetail(details, "Horario", quote.horaInicio() + "–" + quote.horaFin() + overnight);
        addDetail(details, "Invitados", Integer.toString(quote.invitados()));
        addDetail(details, "Dirección", quote.direccion());
        lblApiInfo.setForeground(ThemeMdp.MUTED);
        lblApiInfo.setFont(lblApiInfo.getFont().deriveFont(11f));
        lblApiInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblApiInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (mapaUri == null || !java.awt.Desktop.isDesktopSupported()) return;
                try {
                    java.awt.Desktop.getDesktop().browse(mapaUri);
                } catch (java.io.IOException exception) {
                    lblApiInfo.setText("No se pudo abrir OpenStreetMap; el resto de la reserva sigue disponible.");
                }
            }
        });
        details.add(lblApiInfo);
        return details;
    }

    private void consultarInformacionOpcional(String direccion, LocalDate fecha) {
        new SwingWorker<SalonInfoApiService.DatosSalon, Void>() {
            @Override
            protected SalonInfoApiService.DatosSalon doInBackground() throws Exception {
                return new SalonInfoApiService().consultar(direccion, fecha);
            }

            @Override
            protected void done() {
                try {
                    SalonInfoApiService.DatosSalon data = get();
                    mapaUri = data.mapa();
                    lblApiInfo.setText("<html><u>Ver mapa de OpenStreetMap</u><br>"
                        + (data.pronostico() == null
                            ? "Pronóstico no disponible para esta fecha."
                            : escape(data.pronostico()))
                        + "</html>");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    lblApiInfo.setText("Mapa o pronóstico no disponibles; puedes continuar con la reserva.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    lblApiInfo.setText("Mapa o pronóstico no disponibles; puedes continuar con la reserva.");
                }
            }
        }.execute();
    }

    private JPanel cardServicios() {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        JLabel note = new JLabel("<html>Los servicios y precios se consultan desde MySQL. "
            + "Marca los que quieras incluir; el total se recalcula al instante.</html>");
        note.setForeground(ThemeMdp.MUTED);
        content.add(note, BorderLayout.NORTH);
        JTable table = new JTable(serviceModel);
        ThemeMdp.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeMdp.BORDER));
        content.add(scroll, BorderLayout.CENTER);
        if (serviceModel.getRowCount() == 0) {
            JLabel empty = new JLabel("Este establecimiento no ofrece servicios adicionales.", SwingConstants.CENTER);
            empty.setForeground(ThemeMdp.MUTED);
            content.add(empty, BorderLayout.SOUTH);
        }
        return card("SERVICIOS ADICIONALES", content);
    }

    private JPanel cardDesglose(CotizacionSalonDTO quote) {
        JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));
        content.setOpaque(false);
        BigDecimal hours = BigDecimal.valueOf(Duration.between(quote.horaInicio(), quote.horaFin()).toMinutes());
        if (hours.signum() < 0) hours = hours.add(BigDecimal.valueOf(24L * 60));
        hours = hours.divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
        BigDecimal rental = quote.tarifaBaseHora().multiply(hours);
        BigDecimal guests = quote.costoPorPersona().multiply(BigDecimal.valueOf(quote.invitados()));
        addDetail(content, "Alquiler por " + hours.stripTrailingZeros().toPlainString() + " h", 
            ServicioAdicionalTableModel.formatear(rental));
        addDetail(content, "Costo por invitados", ServicioAdicionalTableModel.formatear(guests));
        servicesValue.setForeground(ThemeMdp.TEXT);
        content.add(servicesValue);
        totalValue.setForeground(ThemeMdp.PRIMARY);
        totalValue.setFont(totalValue.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        content.add(totalValue);
        return card("DESGLOSE ESTIMADO", content);
    }

    private JPanel cardConfirmacion() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        JLabel description = new JLabel("<html>Al confirmar, se guardará una solicitud pendiente en MySQL. "
            + "El proveedor deberá responderla. Los servicios elegidos y sus precios quedarán asociados "
            + "a la reserva.</html>");
        description.setForeground(ThemeMdp.MUTED);
        content.add(description, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancelar");
        JButton confirm = new JButton("Confirmar y solicitar");
        ThemeMdp.styleSecondaryButton(cancel);
        ThemeMdp.stylePrimaryButton(confirm);
        cancel.addActionListener(event -> dispose());
        confirm.addActionListener(event -> {
            confirmed = true;
            dispose();
        });
        buttons.add(cancel);
        buttons.add(confirm);
        content.add(buttons, BorderLayout.SOUTH);
        JPanel card = card("CONFIRMACIÓN REQUERIDA", content);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 4, 1, 1, new Color(0xE3, 0xA0, 0x50)),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeMdp.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
            )
        ));
        return card;
    }

    private JPanel card(String title, JPanel content) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel heading = new JLabel(title);
        heading.setForeground(new Color(0x16, 0x86, 0x7F));
        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static void addDetail(JPanel panel, String label, String value) {
        JLabel row = new JLabel("<html><b>" + escape(label) + ":</b> " + escape(value) + "</html>");
        row.setForeground(ThemeMdp.TEXT);
        panel.add(row);
    }

    private void actualizarTotal(CotizacionSalonDTO quote) {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(quote.horaInicio(), quote.horaFin()).toMinutes());
        if (hours.signum() < 0) hours = hours.add(BigDecimal.valueOf(24L * 60));
        hours = hours.divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
        BigDecimal rental = quote.tarifaBaseHora().multiply(hours);
        BigDecimal guests = quote.costoPorPersona().multiply(BigDecimal.valueOf(quote.invitados()));
        BigDecimal services = serviceModel.totalServicios();
        servicesValue.setText("Servicios: " + ServicioAdicionalTableModel.formatear(services));
        totalEstimado = rental.add(guests).add(services).setScale(2, RoundingMode.HALF_UP);
        totalValue.setText("Total estimado: " + ServicioAdicionalTableModel.formatear(totalEstimado));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<Long> getServiciosSeleccionadosIds() {
        return serviceModel.getServiciosSeleccionadosIds();
    }

    public BigDecimal getTotalEstimado() {
        return totalEstimado;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
