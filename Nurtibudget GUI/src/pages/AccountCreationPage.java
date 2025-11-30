package pages;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import javax.swing.SwingUtilities;
import utils.PythonBridge;

//==============================================================================================================
// AccountCreationPage Class
//==============================================================================================================
public class AccountCreationPage extends JPanel {

    //==============================================================================================================
    // Color constants
    //==============================================================================================================
    private final Color OKSTATE_ORANGE = new Color(244, 125, 32);
    private final Color DARK_GRAY = new Color(45, 45, 45);
    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color WHITE = Color.WHITE;

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public AccountCreationPage(Runnable onAccountCreated, Runnable onBackToLogin) {
        setLayout(new GridBagLayout());
        setBackground(LIGHT_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OKSTATE_ORANGE, 2, true),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        //==========================================================================================================
        // Title
        //==========================================================================================================
        JLabel title = new JLabel("Create Your Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(DARK_GRAY);

        //==========================================================================================================
        // Username Field
        //==========================================================================================================
        JLabel newUsernameLabel = new JLabel("New Username:");
        JTextField newUsernameField = new JTextField(15);
        styleField(newUsernameField);

        //==========================================================================================================
        // Password Field
        //==========================================================================================================
        JLabel newPasswordLabel = new JLabel("New Password:");
        JPasswordField newPasswordField = new JPasswordField(15);
        styleField(newPasswordField);

        //==========================================================================================================
        // Buttons
        //==========================================================================================================
        JButton createAccountButton = styledButton("Create Account", OKSTATE_ORANGE, WHITE);
        createAccountButton.addActionListener(e -> {
            String username = newUsernameField.getText();
            String password = new String(newPasswordField.getPassword());

            new Thread(() -> {
                String script = System.getProperty("user.dir") + File.separator + "Nurtibudget GUI" + File.separator + "SQLHandler" + File.separator + "UserRegistration.py";
                String result = PythonBridge.runWithStdin(script, "register", username, password);

                SwingUtilities.invokeLater(() -> {
                    if (result == null) {
                        JOptionPane.showMessageDialog(this, "Registration error (couldn't run registrar)", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String out = result.trim();
                    switch (out) {
                        case "CREATED":
                            JOptionPane.showMessageDialog(this, "Account created!");
                            onAccountCreated.run();
                            break;
                        case "EXISTS":
                            JOptionPane.showMessageDialog(this, "User already exists.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
                            break;
                        default:
                            JOptionPane.showMessageDialog(this, "Registration error: " + out, "Registration Failed", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        });

        JButton backButton = styledButton("Back to Login", DARK_GRAY, WHITE);
        backButton.addActionListener(e -> onBackToLogin.run());

        //==========================================================================================================
        // Layout inside card
        //==========================================================================================================
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        card.add(newUsernameLabel, gbc);

        gbc.gridx = 1;
        card.add(newUsernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        card.add(newPasswordLabel, gbc);

        gbc.gridx = 1;
        card.add(newPasswordField, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(createAccountButton, gbc);

        gbc.gridy = 4;
        card.add(backButton, gbc);

        add(card);
    }

    //==============================================================================================================
    // Field styling helper
    //==============================================================================================================
    private void styleField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(WHITE);
    }

    //==============================================================================================================
    // Styled button helper
    //==============================================================================================================
    private JButton styledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bg.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bg);
            }
        });

        return button;
    }

    

}
