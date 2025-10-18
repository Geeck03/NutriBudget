import javax.swing.*;
import java.awt.*;

public class LoginPage extends JPanel {

    private final Color OKSTATE_ORANGE = new Color(244, 125, 32);
    private final Color DARK_GRAY = new Color(45, 45, 45);
    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color WHITE = Color.WHITE;

    //==================================================================================================================

    public LoginPage(Runnable onLoginSuccess, Runnable onCreateAccount) {
        setLayout(new GridBagLayout());
        setBackground(LIGHT_GRAY);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(WHITE);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OKSTATE_ORANGE, 2, true),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JLabel title = new JLabel("Welcome to Nutribudget");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(DARK_GRAY);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JLabel username = new JLabel("Username:");
        JTextField usernameField = new JTextField(15);
        styleField(usernameField);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JLabel password = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(15);
        styleField(passwordField);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JButton confirmButton = styledButton("Login", OKSTATE_ORANGE, WHITE);
        confirmButton.addActionListener(e -> onLoginSuccess.run());

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JButton createAccountButton = styledButton("Create Account", DARK_GRAY, WHITE);
        createAccountButton.addActionListener(e -> onCreateAccount.run());

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Layout inside card

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        card.add(username, gbc);

        gbc.gridx = 1;
        card.add(usernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        card.add(password, gbc);

        gbc.gridx = 1;
        card.add(passwordField, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(confirmButton, gbc);

        gbc.gridy = 4;
        card.add(createAccountButton, gbc);

        add(card);
    }

    //==================================================================================================================

    private void styleField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(WHITE);
    }

    //==================================================================================================================

    private JButton styledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        button.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bg.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bg);
            }
        });

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        return button;
    }

    //==================================================================================================================

}
