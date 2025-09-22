import javax.swing.*;
import java.awt.*;

public class LoginPage extends JPanel {
    public LoginPage(Runnable onLoginSuccess) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel username = new JLabel("Username:");
        JTextField usernameField = new JTextField(15);

        JLabel password = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(15);

        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> onLoginSuccess.run());

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(username, gbc);

        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(password, gbc);

        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(confirmButton, gbc);
    }
}
