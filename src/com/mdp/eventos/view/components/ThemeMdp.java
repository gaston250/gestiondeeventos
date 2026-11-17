package com.mdp.eventos.view.components;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

public final class ThemeMdp {
    public static final Color NAVY = new Color(0x12, 0x32, 0x4A);
    public static final Color TEAL = new Color(0x27, 0xA8, 0xA1);
    public static final Color BACKGROUND = new Color(0xF7, 0xF9, 0xFA);
    public static final Color BORDER = new Color(0xCC, 0xD7, 0xDC);
    public static final Color TEXT = new Color(0x17, 0x28, 0x3B);
    public static final Color MUTED = new Color(0x61, 0x71, 0x80);
    public static final Color PRIMARY = new Color(0x12, 0x6B, 0x77);
    public static final Color SELECTED = new Color(0xE0, 0xF1, 0xEF);

    private static final Color HEADER_BACKGROUND = new Color(0xEF, 0xF3, 0xF4);
    private static final Color CELL_TEXT = new Color(0x42, 0x54, 0x5F);
    private static final Color STRIPE = new Color(0x14, 0x91, 0x8A);

    private ThemeMdp() {
    }

    public static void applyGlobal() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
    }

    public static void styleWindow(JPanel root) {
        root.setBackground(BACKGROUND);
        root.setOpaque(true);
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
    }

    public static void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY, Color.WHITE, new Color(0x0E, 0x60, 0x6B));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
    }

    public static void styleSecondaryButton(JButton button) {
        styleButton(button, Color.WHITE, new Color(0x27, 0x45, 0x57), new Color(0xAE, 0xBF, 0xC7));
    }

    public static void styleDangerButton(JButton button) {
        styleButton(button, new Color(0xFF, 0xF7, 0xF5), new Color(0x9C, 0x3C, 0x3C),
            new Color(0xD9, 0xA3, 0x9E));
    }

    private static void styleButton(JButton button, Color background, Color foreground, Color borderColor) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static void styleTable(JTable table) {
        table.setBackground(Color.WHITE);
        table.setForeground(CELL_TEXT);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(new Color(0x1D, 0x3D, 0x48));
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BACKGROUND);
        header.setForeground(new Color(0x4A, 0x5D, 0x68));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setReorderingAllowed(false);
        TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((source, value, selected, focused, row, column) -> {
            Component cell = defaultHeaderRenderer.getTableCellRendererComponent(
                source, value, selected, focused, row, column
            );
            if (cell instanceof JLabel label) {
                label.setText(value == null ? "" : value.toString().toUpperCase(java.util.Locale.ROOT));
                label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
                label.setForeground(new Color(0x4A, 0x5D, 0x68));
                label.setBackground(HEADER_BACKGROUND);
                label.setOpaque(true);
            }
            return cell;
        });

        TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, (source, value, selected, focused, row, column) -> {
            Component cell = defaultRenderer.getTableCellRendererComponent(
                source, value, selected, focused, row, column
            );
            if (cell instanceof JLabel label) {
                label.setBorder(selectionBorder(selected, column == 0));
                label.setBackground(selected ? SELECTED : isHovered(source, row)
                    ? new Color(0xF2, 0xF8, 0xF8) : Color.WHITE);
                label.setForeground(selected ? new Color(0x1D, 0x3D, 0x48) : CELL_TEXT);
                label.setOpaque(true);
            }
            return cell;
        });
        table.putClientProperty("mdp.hoverRow", -1);
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                if (!Objects.equals(table.getClientProperty("mdp.hoverRow"), row)) {
                    table.putClientProperty("mdp.hoverRow", row);
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                table.putClientProperty("mdp.hoverRow", -1);
                table.repaint();
            }
        });
    }

    public static boolean isHovered(JTable table, int row) {
        return Objects.equals(table.getClientProperty("mdp.hoverRow"), row);
    }

    public static StatusBadgeCellRenderer statusRenderer() {
        return new StatusBadgeCellRenderer();
    }

    public static JPanel createHeader(String section, Runnable onLogout) {
        return createHeader(section, onLogout, null, null);
    }

    public static JPanel createHeader(
        String section, Runnable onLogout, String navigationLabel, Runnable onNavigate
    ) {
        Objects.requireNonNull(onLogout, "onLogout");
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 4, 0, TEAL));
        header.setPreferredSize(new Dimension(1000, 72));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        brand.setOpaque(false);
        brand.add(new BrandMark());
        JLabel name = new JLabel("<html><b>Eventos MDP</b><br><span style='color:#D7E5EA'>"
            + escapeHtml(section) + "</span></html>");
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(14f));
        brand.add(name);
        header.add(brand, BorderLayout.WEST);

        Usuario usuario = SesionActual.getUsuario().orElse(null);
        String nombre = usuario == null ? "Sesión no iniciada" : usuario.getNombreCompleto();
        String rol = usuario == null || usuario.getRol() == null
            ? ""
            : rolVisible(usuario.getRol().getNombre());

        JPanel user = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 17));
        user.setOpaque(false);
        if (navigationLabel != null && onNavigate != null) {
            JButton navigate = new JButton(navigationLabel);
            styleSecondaryButton(navigate);
            navigate.addActionListener(event -> onNavigate.run());
            user.add(navigate);
        }
        JLabel identity = new JLabel("<html><b>Hola, " + escapeHtml(nombre) + "</b><br>"
            + "<span style='color:#D7E5EA'>Vista " + escapeHtml(rol) + "</span></html>");
        identity.setForeground(Color.WHITE);
        user.add(identity);
        JButton logout = new JButton("Cerrar sesión");
        styleSecondaryButton(logout);
        logout.addActionListener(event -> {
            onLogout.run();
            java.awt.Window window = SwingUtilities.getWindowAncestor(logout);
            if (window != null) window.dispose();
        });
        user.add(logout);
        header.add(user, BorderLayout.EAST);
        return header;
    }

    public static Border selectionBorder(boolean selected, boolean firstColumn) {
        if (selected && firstColumn) {
            return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, STRIPE),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)
            );
        }
        return BorderFactory.createEmptyBorder(0, 12, 0, 8);
    }

    private static String rolVisible(String rol) {
        if (Rol.ROLE_CLIENTE.equals(rol)) return "Cliente";
        if (Rol.ROLE_PROVEEDOR.equals(rol)) return "Proveedor";
        if (Rol.ROLE_ADMIN.equals(rol)) return "Administrador";
        return rol;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class BrandMark extends JPanel {
        private BrandMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x4B, 0xC5, 0xBE));
            g.fillRoundRect(2, 5, 6, 20, 3, 3);
            g.setColor(Color.WHITE);
            g.fillRoundRect(12, 2, 6, 23, 3, 3);
            g.setColor(new Color(0xE8, 0xA3, 0x5B));
            g.fillRoundRect(22, 9, 6, 16, 3, 3);
            g.dispose();
        }
    }
}
