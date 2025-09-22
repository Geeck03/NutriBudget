import javax.swing.*;
import java.awt.*;
import pages.*;

public class Main {
    private JFrame frame;
    private final int pageTotal = 4;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().loginScreen());
    }

    //==================================================================================================================
    // Show login screen

    private void loginScreen() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(new LoginPage(this::mainApp));
        frame.setVisible(true);
    }

    //==================================================================================================================
    // Swap to main app

    private void mainApp() {
        Dimension prevSize = frame.getSize();
        Point prevLocation = frame.getLocationOnScreen();

        frame.getContentPane().removeAll();
        frame.setTitle("Nutribudget GUI");
        frame.setLayout(new BorderLayout());

        // Sidebar
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(Color.LIGHT_GRAY);
        buttonPanel.setPreferredSize(new Dimension(140, 600));


    //==================================================================================================================
    // Content area

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        cardPanel.add(new Page1(), "Page1");
        cardPanel.add(new Page2(), "Page2");
        cardPanel.add(new Page3(), "Page3");
        cardPanel.add(new Page4(), "Page4");


        //==============================================================================================================

        for (int i = 1; i <= pageTotal; i++) {
            int pageNum = i;
            JButton button = pageButton("/images/icon" + i + ".png", 100, 60);
            if (button == null) {
                button = new JButton("Page " + i);
            }

            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(120, 80));
            button.addActionListener(e -> cardLayout.show(cardPanel, "Page" + pageNum));

            buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            buttonPanel.add(button);
        }

        //==============================================================================================================

        frame.add(buttonPanel, BorderLayout.WEST);
        frame.add(cardPanel, BorderLayout.CENTER);

        frame.pack();

        //SIZE and Position
        int newWidth = 1200;
        int newHeight = 800;

        // Calculate center point
        int centerX = prevLocation.x + prevSize.width / 2;
        int centerY = prevLocation.y + prevSize.height / 2;

        // Calculate new top-left
        int newX = centerX - newWidth / 2;
        int newY = centerY - newHeight / 2;

        frame.setSize(newWidth, newHeight);
        frame.setLocation(newX, newY);

        frame.revalidate();
        frame.repaint();

        cardLayout.show(cardPanel, "Page1");
    }

//======================================================================================================================

// image check
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
