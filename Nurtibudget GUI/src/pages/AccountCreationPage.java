package pages;

import javax.swing.*;
import java.awt.*;

public class AccountCreationPage extends JPanel {
    public AccountCreationPage(Runnable onAccountCreated, Runnable onBackToLogin) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel newUsernameLabel = new JLabel("New Username:");
        JTextField newUsernameField = new JTextField(15);

        JLabel newPasswordLabel = new JLabel("New Password:");
        JPasswordField newPasswordField = new JPasswordField(15);

        JButton createAccountButton = new JButton("Create Account");
        createAccountButton.addActionListener(e -> {

            JOptionPane.showMessageDialog(this, "Account created!");
            onAccountCreated.run();
        });

        JButton backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> onBackToLogin.run());

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(newUsernameLabel, gbc);
        gbc.gridx = 1;
        add(newUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(newPasswordLabel, gbc);
        gbc.gridx = 1;
        add(newPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(createAccountButton, gbc);

        gbc.gridy = 3;
        add(backButton, gbc);
    }
}
