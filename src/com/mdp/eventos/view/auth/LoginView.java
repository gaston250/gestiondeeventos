package com.mdp.eventos.view.auth;

import com.mdp.eventos.controller.AuthController;
import com.mdp.eventos.dto.RegistroUsuarioDTO;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.view.components.ThemeMdp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Consumer;

public class LoginView extends JFrame {
    private JPanel rootPanel = new JPanel(new GridBagLayout());
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnRegistrarse;
    private JLabel lblEstado;
    private Consumer<Usuario> onLoginCorrecto = usuario -> { };

    public LoginView() {
        super("Eventos MDP | Iniciar sesión");
        ThemeMdp.applyGlobal();
        inicializarInterfaz();
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        setSize(1020, 620);
        setLocationRelativeTo(null);
    }

    private void inicializarInterfaz() {
        JPanel marca = new JPanel(new BorderLayout(0, 28));
        marca.setBackground(ThemeMdp.NAVY);
        marca.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 4, ThemeMdp.TEAL),
            BorderFactory.createEmptyBorder(42, 42, 34, 36)
        ));

        JPanel brandLockup = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brandLockup.setOpaque(false);
        brandLockup.add(new LogoBars());
        JLabel brandName = new JLabel("<html><b>Eventos MDP</b><br><span style='color:#D7E5EA'>"
            + "Gestión de salones</span></html>");
        brandName.setForeground(Color.WHITE);
        brandName.setFont(brandName.getFont().deriveFont(16f));
        brandLockup.add(brandName);
        marca.add(brandLockup, BorderLayout.NORTH);

        JPanel pitch = new JPanel(new GridLayout(3, 1, 0, 12));
        pitch.setOpaque(false);
        JLabel headline = new JLabel("<html><div style='width:310px'>Todo tu evento,<br>en un solo lugar.</div></html>");
        headline.setForeground(Color.WHITE);
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 32f));
        JLabel description = new JLabel("<html><div style='width:300px'>Encuentra salones, compara cotizaciones "
            + "y organiza tu próximo evento en Mar del Plata.</div></html>");
        description.setForeground(new Color(0xD7, 0xE5, 0xEA));
        description.setFont(description.getFont().deriveFont(15f));
        pitch.add(headline);
        pitch.add(description);
        pitch.add(new JLabel());
        marca.add(pitch, BorderLayout.CENTER);

        JLabel connection = new JLabel("●  MySQL: conexión y credenciales se validan al ingresar");
        connection.setForeground(new Color(0xD7, 0xE5, 0xEA));
        connection.setFont(connection.getFont().deriveFont(12f));
        marca.add(connection, BorderLayout.SOUTH);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(ThemeMdp.BACKGROUND);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeMdp.BORDER),
            BorderFactory.createEmptyBorder(32, 34, 30, 34)
        ));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 0);

        JLabel eyebrow = new JLabel("BIENVENIDO");
        eyebrow.setForeground(new Color(0x16, 0x86, 0x7F));
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 10f));
        c.gridy = 0;
        card.add(eyebrow, c);
        JLabel title = new JLabel("Iniciar sesión");
        title.setForeground(ThemeMdp.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        c.gridy = 1;
        c.insets = new Insets(0, 0, 22, 0);
        card.add(title, c);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Correo electrónico"), c);
        txtEmail = new JTextField(24);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 16, 0);
        card.add(txtEmail, c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Contraseña"), c);
        txtPassword = new JPasswordField(24);
        c.gridy = 5;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(txtPassword, c);

        lblEstado = new JLabel(" ");
        lblEstado.setForeground(new Color(0x9A, 0x3E, 0x3E));
        lblEstado.setPreferredSize(new Dimension(300, 30));
        c.gridy = 6;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(lblEstado, c);
        btnIngresar = new JButton("Iniciar sesión");
        ThemeMdp.stylePrimaryButton(btnIngresar);
        c.gridy = 7;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(btnIngresar, c);
        JPanel registration = new JPanel(new BorderLayout(8, 0));
        registration.setOpaque(false);
        JLabel accountHint = new JLabel("¿Nuevo usuario?");
        accountHint.setForeground(ThemeMdp.MUTED);
        accountHint.setFont(accountHint.getFont().deriveFont(12f));
        registration.add(accountHint, BorderLayout.CENTER);
        btnRegistrarse = new JButton("Crear cuenta");
        ThemeMdp.styleSecondaryButton(btnRegistrarse);
        registration.add(btnRegistrarse, BorderLayout.EAST);
        c.gridy = 8;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(registration, c);

        GridBagConstraints centered = new GridBagConstraints();
        centered.gridx = 0;
        centered.gridy = 0;
        centered.weightx = 1;
        centered.weighty = 1;
        centered.anchor = GridBagConstraints.CENTER;
        centered.fill = GridBagConstraints.HORIZONTAL;
        centered.insets = new Insets(40, 40, 40, 40);
        right.add(card, centered);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.42;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        rootPanel.add(marca, left);
        GridBagConstraints rightSide = new GridBagConstraints();
        rightSide.gridx = 1;
        rightSide.gridy = 0;
        rightSide.weightx = 0.58;
        rightSide.weighty = 1;
        rightSide.fill = GridBagConstraints.BOTH;
        rootPanel.add(right, rightSide);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ThemeMdp.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    public void setController(AuthController controller) {
        Objects.requireNonNull(controller, "controller");
        btnIngresar.addActionListener(event -> controller.ingresar());
        txtPassword.addActionListener(event -> controller.ingresar());
        btnRegistrarse.addActionListener(event -> controller.registrar(mostrarFormularioRegistro()));
    }

    public void setOnLoginCorrecto(Consumer<Usuario> onLoginCorrecto) {
        this.onLoginCorrecto = Objects.requireNonNull(onLoginCorrecto, "onLoginCorrecto");
    }

    public String getEmailIngresado() {
        return txtEmail.getText().trim();
    }

    public char[] getPasswordIngresada() {
        return txtPassword.getPassword();
    }

    public void limpiarPassword() {
        txtPassword.setText("");
    }

    public void setAutenticando(boolean autenticando) {
        btnIngresar.setEnabled(!autenticando);
        btnRegistrarse.setEnabled(!autenticando);
        txtEmail.setEnabled(!autenticando);
        txtPassword.setEnabled(!autenticando);
        lblEstado.setForeground(autenticando ? ThemeMdp.MUTED : new Color(0x9A, 0x3E, 0x3E));
        lblEstado.setText(autenticando ? "Conectando y verificando credenciales..." : " ");
    }

    private RegistroUsuarioDTO mostrarFormularioRegistro() {
        JTextField name = new JTextField(22);
        JTextField email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JComboBox<String> role = new JComboBox<>(new String[]{"Cliente", "Proveedor"});
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(5, 5, 5, 10);
        form.add(new JLabel("Nombre completo"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(name, c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Correo"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(email, c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Contraseña"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(password, c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Tipo de cuenta"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(role, c);
        int answer = javax.swing.JOptionPane.showConfirmDialog(
            this, form, "Crear cuenta", javax.swing.JOptionPane.OK_CANCEL_OPTION,
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );
        if (answer != javax.swing.JOptionPane.OK_OPTION) return null;
        char[] chars = password.getPassword();
        String secret = new String(chars);
        java.util.Arrays.fill(chars, '\0');
        return new RegistroUsuarioDTO(
            name.getText(), email.getText(), secret, null,
            "Proveedor".equals(role.getSelectedItem())
                ? com.mdp.eventos.model.Rol.ROLE_PROVEEDOR
                : com.mdp.eventos.model.Rol.ROLE_CLIENTE
        );
    }

    public void registroCorrecto(String email) {
        txtEmail.setText(email);
        limpiarPassword();
        mostrarError("Cuenta creada. Ya puedes iniciar sesión.");
        lblEstado.setForeground(new Color(0x28, 0x6B, 0x56));
    }

    public void mostrarError(String mensaje) {
        lblEstado.setForeground(new Color(0x9A, 0x3E, 0x3E));
        lblEstado.setText("<html>" + mensaje.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;") + "</html>");
    }

    public void loginCorrecto(Usuario usuario) {
        setVisible(false);
        onLoginCorrecto.accept(usuario);
    }

    private static final class LogoBars extends JPanel {
        private LogoBars() {
            setOpaque(false);
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x4B, 0xC5, 0xBE));
            g.fillRoundRect(1, 7, 7, 23, 3, 3);
            g.setColor(Color.WHITE);
            g.fillRoundRect(13, 2, 7, 28, 3, 3);
            g.setColor(new Color(0xE8, 0xA3, 0x5B));
            g.fillRoundRect(25, 11, 7, 19, 3, 3);
            g.dispose();
        }
    }
}
