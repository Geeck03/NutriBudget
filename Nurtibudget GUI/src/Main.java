import java.awt.*;
import javax.swing.*;
import pages.*;

//======================================================================================================================
// Main Class
//======================================================================================================================
public class Main {

    //==============================================================================================================
    // Fields and Constants
    //==============================================================================================================
    private JFrame frame;
    private final Color OKSTATE_ORANGE = new Color(244, 125, 32);
    private final Color DARK_GRAY = new Color(45, 45, 45);
    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color WHITE = Color.WHITE;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    //==============================================================================================================
    // Main Entry Point
    //==============================================================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().loginScreen());
    }

    //==================================================================================================================
    // Login Screen Setup
    //==================================================================================================================
    private void loginScreen() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        //--------------------------------------------------------------------------------------------------------------
        // Pages
        //--------------------------------------------------------------------------------------------------------------
        LoginPage loginPage = new LoginPage(
                this::mainApp,
                this::showAccountCreation
        );

        AccountCreationPage createPage = new AccountCreationPage(
                this::showLogin,
                this::showLogin
        );

        //--------------------------------------------------------------------------------------------------------------
        // Add Pages to Main Panel
        //--------------------------------------------------------------------------------------------------------------
        mainPanel.add(loginPage, "login");
        mainPanel.add(createPage, "create");

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
        cardLayout.show(mainPanel, "login");
    }

    //==================================================================================================================
    // Switch Between Login and Account Creation
    //==================================================================================================================
    private void showAccountCreation() {
        cardLayout.show(mainPanel, "create");
    }

    private void showLogin() {
        cardLayout.show(mainPanel, "login");
    }

    //==================================================================================================================
    // Main Application
    //==================================================================================================================
    private void mainApp() {
        Dimension prevSize = frame.getSize();
        Point prevLocation = frame.getLocationOnScreen();

        //--------------------------------------------------------------------------------------------------------------
        // Frame Setup
        //--------------------------------------------------------------------------------------------------------------
        frame.getContentPane().removeAll();
        frame.setTitle("Nutribudget GUI");
        frame.setLayout(new BorderLayout());

        //--------------------------------------------------------------------------------------------------------------
        // Header
        //--------------------------------------------------------------------------------------------------------------
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(OKSTATE_ORANGE);
        header.setPreferredSize(new Dimension(frame.getWidth(), 60));

        JLabel title = new JLabel("Nutribudget");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        header.add(title, BorderLayout.WEST);
        frame.add(header, BorderLayout.NORTH);

        //--------------------------------------------------------------------------------------------------------------
        // Sidebar Button Panel
        //--------------------------------------------------------------------------------------------------------------
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(DARK_GRAY);
        buttonPanel.setPreferredSize(new Dimension(140, 800));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        //--------------------------------------------------------------------------------------------------------------
        // Card Layout for Pages
        //--------------------------------------------------------------------------------------------------------------
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(LIGHT_GRAY);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        cardPanel.add(new Page1(), "Page1");
        cardPanel.add(new Page2(), "Page2");
        cardPanel.add(new Page3(), "Page3");
        cardPanel.add(new Page4(), "Page4");


        cardPanel.add(new SuggestionsPage(), "Suggestions");

        cardPanel.add(new AccountInfoPage(), "AccountInfo");

        //--------------------------------------------------------------------------------------------------------------
        // Sidebar Buttons (Pages 1–4)
        //--------------------------------------------------------------------------------------------------------------
        for (int i = 1; i <= 4; i++) {
            int pageNum = i;
            JButton button = pageButton("/pages/images/icon" + i + ".png", 64, 64);

            if (button == null) {
                button = new JButton("Page " + i);
                button.setForeground(WHITE);
                button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            }

            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setBackground(DARK_GRAY);
            button.setOpaque(true);
            button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton finalButton = button;

            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    finalButton.setBackground(OKSTATE_ORANGE);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    finalButton.setBackground(DARK_GRAY);
                }
            });

            button.addActionListener(e -> cardLayout.show(cardPanel, "Page" + pageNum));

            buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            buttonPanel.add(button);
        }

        //--------------------------------------------------------------------------------------------------------------
        // Suggestions Button
        //--------------------------------------------------------------------------------------------------------------
        JButton suggestionsButton = pageButton("/pages/images/icon5.png", 64, 64);

        if (suggestionsButton == null) {
            suggestionsButton = new JButton("Suggestions");
            suggestionsButton.setForeground(WHITE);
            suggestionsButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        suggestionsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        suggestionsButton.setBackground(DARK_GRAY);
        suggestionsButton.setOpaque(true);
        suggestionsButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton finalSuggestionsButton = suggestionsButton;

        suggestionsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                finalSuggestionsButton.setBackground(OKSTATE_ORANGE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                finalSuggestionsButton.setBackground(DARK_GRAY);
            }
        });

        suggestionsButton.addActionListener(e -> cardLayout.show(cardPanel, "Suggestions"));
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        buttonPanel.add(suggestionsButton);

        //--------------------------------------------------------------------------------------------------------------
        // Account Info Button (bottom)
        //--------------------------------------------------------------------------------------------------------------
        buttonPanel.add(Box.createVerticalGlue());
        JButton accountButton = pageButton("/pages/images/account_icon.png", 64, 64);

        if (accountButton == null) {
            accountButton = new JButton("Account");
            accountButton.setForeground(WHITE);
            accountButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        accountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        accountButton.setBackground(DARK_GRAY);
        accountButton.setOpaque(true);
        accountButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton finalAccountButton = accountButton;

        accountButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                finalAccountButton.setBackground(OKSTATE_ORANGE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                finalAccountButton.setBackground(DARK_GRAY);
            }
        });

        accountButton.addActionListener(e -> cardLayout.show(cardPanel, "AccountInfo"));
        buttonPanel.add(accountButton);

        //--------------------------------------------------------------------------------------------------------------
        // Final Frame Assembly
        //--------------------------------------------------------------------------------------------------------------
        frame.add(buttonPanel, BorderLayout.WEST);
        frame.add(cardPanel, BorderLayout.CENTER);

        int newWidth = 1200;
        int newHeight = 800;
        int centerX = prevLocation.x + prevSize.width / 2;
        int centerY = prevLocation.y + prevSize.height / 2;

        frame.setSize(newWidth, newHeight);
        frame.setLocation(centerX - newWidth / 2, centerY - newHeight / 2);
        frame.revalidate();
        frame.repaint();
        cardLayout.show(cardPanel, "Page1");
    }

    //==================================================================================================================
    // Sidebar Page Buttons
    //==================================================================================================================
    private JButton pageButton(String resourcePath, int width, int height) {
        java.net.URL imgURL = getClass().getResource(resourcePath);

        if (imgURL == null) {
            System.err.println("Image resource not found: " + resourcePath);
            return null;
        }

        ImageIcon originalIcon = new ImageIcon(imgURL);
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JButton button = new JButton(scaledIcon);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    //==================================================================================================================
}
