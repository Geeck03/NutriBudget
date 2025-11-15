package pages;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

//======================================================================================================================
// Account Info Page
//======================================================================================================================
public class AccountInfoPage extends JPanel {

    private JLabel profilePicLabel;
    private ImageIcon defaultIcon;
    private ImageIcon userIcon;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);

    private static final String DEFAULT_IMAGE_PATH = "/pages/images/default_user.png";
    private static final String USER_IMAGE_PATH = "src/pages/images/profile_picture.png";

    //==================================================================================================================
    // Constructor
    //==================================================================================================================
    public AccountInfoPage() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(LIGHT_GRAY);
        setBorder(new EmptyBorder(50, 50, 50, 50));

        JLabel title = new JLabel("Account Information", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(30));

        //--------------------------------------------------------------------------------------------------------------
        // Load Profile Picture
        //--------------------------------------------------------------------------------------------------------------
        defaultIcon = loadImageIcon(DEFAULT_IMAGE_PATH, 180, 180);
        userIcon = loadUserImage();

        profilePicLabel = new JLabel(userIcon != null ? userIcon : defaultIcon);
        profilePicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profilePicLabel.setBorder(new LineBorder(Color.GRAY, 2));
        add(profilePicLabel);
        add(Box.createVerticalStrut(20));

        //--------------------------------------------------------------------------------------------------------------
        // Buttons Panel (Change / Remove)
        //--------------------------------------------------------------------------------------------------------------
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton changeBtn = new JButton("Change Profile Picture");
        JButton removeBtn = new JButton("Remove Profile Picture");

        changeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        removeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Upload New Picture
        changeBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                try {
                    Path dest = Paths.get(USER_IMAGE_PATH);
                    Files.copy(selectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

                    userIcon = new ImageIcon(new ImageIcon(USER_IMAGE_PATH)
                            .getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH));
                    profilePicLabel.setIcon(userIcon);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error saving profile picture: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Reset to Default
        removeBtn.addActionListener(e -> {
            try {
                Files.deleteIfExists(Paths.get(USER_IMAGE_PATH));
                profilePicLabel.setIcon(defaultIcon);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error removing profile picture: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(changeBtn);
        btnPanel.add(removeBtn);
        add(btnPanel);
        add(Box.createVerticalStrut(40));

        //--------------------------------------------------------------------------------------------------------------
        // Placeholder Account Info
        //--------------------------------------------------------------------------------------------------------------
        JLabel nameLabel = new JLabel("Username: example_user");
        JLabel emailLabel = new JLabel("Email: example@email.com");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(nameLabel);
        add(Box.createVerticalStrut(10));
        add(emailLabel);
    }

    //==================================================================================================================
    // Load Existing Saved Profile Picture
    //==================================================================================================================
    private ImageIcon loadUserImage() {
        File imgFile = new File(USER_IMAGE_PATH);
        if (imgFile.exists()) {
            return new ImageIcon(new ImageIcon(USER_IMAGE_PATH)
                    .getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH));
        }
        return null;
    }

    //==================================================================================================================
    // Load Resource Image Icon
    //==================================================================================================================
    private ImageIcon loadImageIcon(String path, int width, int height) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL);
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } else {
            System.err.println("Image not found: " + path);
            return null;
        }
    }
}
