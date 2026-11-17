package com.mdp.eventos.view.components;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class StatusBadgeCellRenderer extends JPanel implements TableCellRenderer {
    private final PillLabel badge = new PillLabel();

    public StatusBadgeCellRenderer() {
        super(new BorderLayout());
        setOpaque(true);
        setBorder(new EmptyBorder(0, 12, 0, 8));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        add(badge, BorderLayout.WEST);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean selected, boolean hasFocus, int row, int column
    ) {
        String status = value instanceof Boolean activo
            ? (activo ? "Activo" : "Inactivo")
            : value == null ? "" : value.toString();
        badge.setText(status);
        Colors colors = colorsFor(status);
        badge.setPillColor(colors.background());
        badge.setForeground(colors.foreground());
        badge.setFont(table.getFont().deriveFont(12f));
        setBackground(selected ? ThemeMdp.SELECTED : ThemeMdp.isHovered(table, row)
            ? new Color(0xF2, 0xF8, 0xF8) : Color.WHITE);
        setBorder(ThemeMdp.selectionBorder(selected, column == 0));
        return this;
    }

    private static Colors colorsFor(String status) {
        return switch (status.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "confirmada", "disponible", "activo" ->
                new Colors(new Color(0xDF, 0xF1, 0xE9), new Color(0x28, 0x6B, 0x56));
            case "ok" ->
                new Colors(new Color(0xDF, 0xF1, 0xE9), new Color(0x28, 0x6B, 0x56));
            case "pendiente" ->
                new Colors(new Color(0xFF, 0xF0, 0xD8), new Color(0x81, 0x53, 0x1B));
            case "rechazada", "inactivo", "error" ->
                new Colors(new Color(0xFB, 0xE5, 0xE3), new Color(0x9A, 0x3E, 0x3E));
            case "cancelada" ->
                new Colors(new Color(0xEC, 0xEF, 0xF0), new Color(0x64, 0x71, 0x78));
            case "vencida" ->
                new Colors(new Color(0xEC, 0xEF, 0xF0), new Color(0x64, 0x71, 0x78));
            case "finalizada" ->
                new Colors(new Color(0xE4, 0xF0, 0xF6), new Color(0x38, 0x65, 0x80));
            default -> new Colors(new Color(0xE8, 0xED, 0xEF), new Color(0x61, 0x71, 0x80));
        };
    }

    private record Colors(Color background, Color foreground) {
    }

    private static final class PillLabel extends JLabel {
        private Color pillColor = Color.LIGHT_GRAY;

        private void setPillColor(Color color) {
            pillColor = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(pillColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
