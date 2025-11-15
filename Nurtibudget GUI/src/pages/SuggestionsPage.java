package pages;

import javax.swing.*;
import java.awt.*;

public class SuggestionsPage extends JPanel {

    private final Color OKSTATE_ORANGE = new Color(244, 125, 32);
    private final Color DARK_GRAY = new Color(45, 45, 45);
    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color WHITE = Color.WHITE;

    public SuggestionsPage() {
        // Match the layout of Page1, Page2, Page3, Page4
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ------------------------------------------------------------------------------------
        // Title Area (Same format as other pages)
        // ------------------------------------------------------------------------------------
        JLabel title = new JLabel("Suggestions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(DARK_GRAY);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(LIGHT_GRAY);
        titlePanel.add(title, BorderLayout.CENTER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        add(titlePanel, BorderLayout.NORTH);

        // ------------------------------------------------------------------------------------
        // Main Content Area
        // ------------------------------------------------------------------------------------
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(LIGHT_GRAY);

        // Button
        JButton generateButton = new JButton("Generate a Suggestion");
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        generateButton.setForeground(WHITE);
        generateButton.setBackground(OKSTATE_ORANGE);
        generateButton.setFocusPainted(false);
        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateButton.setBorder(BorderFactory.createEmptyBorder(15, 35, 15, 35));
        generateButton.setOpaque(true);

        // Hover effect
        generateButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                generateButton.setBackground(DARK_GRAY);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                generateButton.setBackground(OKSTATE_ORANGE);
            }
        });

        // Placeholder click action
        generateButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    this,
                    "Suggestion generation coming soon!",
                    "Coming Soon",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        contentPanel.add(generateButton);

        // Center content vertically
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(LIGHT_GRAY);
        centerWrapper.add(contentPanel);

        add(centerWrapper, BorderLayout.CENTER);
    }
}
