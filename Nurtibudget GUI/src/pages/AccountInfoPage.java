package pages;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;

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
        // Outer scroll pane to make the entire page scrollable
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(LIGHT_GRAY);
        contentPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        JLabel title = new JLabel("Account Information", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(title);
        contentPanel.add(Box.createVerticalStrut(30));

        // Commented out placeholder
        // Load Profile Picture
        /*
        defaultIcon = loadImageIcon(DEFAULT_IMAGE_PATH, 180, 180);
        userIcon = loadUserImage();

        profilePicLabel = new JLabel(userIcon != null ? userIcon : defaultIcon);
        profilePicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profilePicLabel.setBorder(new LineBorder(Color.GRAY, 2));
        contentPanel.add(profilePicLabel);
        contentPanel.add(Box.createVerticalStrut(20));

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
        contentPanel.add(btnPanel);
        contentPanel.add(Box.createVerticalStrut(40));
        */
        //--------------------------------------------------------------------------------------------------------------
        // Dietary Restrictions Section
        //--------------------------------------------------------------------------------------------------------------
        JLabel dietLabel = new JLabel("Dietary Restrictions");
        dietLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dietLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(dietLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Create checkboxes panel
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new GridLayout(7, 2, 15, 15));
        checkboxPanel.setBackground(LIGHT_GRAY);
        checkboxPanel.setMaximumSize(new Dimension(400, 280));

        JCheckBox[] dietaryCheckboxes = new JCheckBox[14];
        String[] dietaryOptions = {
            "Vegetarian", "Vegan", "Pescatarian", "Kosher",
            "Halal", "No Dairy", "No Egg", "No Fish",
            "No Shellfish", "No Tree Nuts", "No Peanuts",
            "No Wheat/Gluten", "No Soy", "No Sesame"
        };

        for (int i = 0; i < dietaryOptions.length; i++) {
            dietaryCheckboxes[i] = new JCheckBox(dietaryOptions[i]);
            dietaryCheckboxes[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dietaryCheckboxes[i].setBackground(LIGHT_GRAY);
            checkboxPanel.add(dietaryCheckboxes[i]);
        }

        JPanel checkboxWrapper = new JPanel();
        checkboxWrapper.setLayout(new BoxLayout(checkboxWrapper, BoxLayout.X_AXIS));
        checkboxWrapper.setBackground(LIGHT_GRAY);
        checkboxWrapper.add(Box.createHorizontalGlue());
        checkboxWrapper.add(checkboxPanel);
        checkboxWrapper.add(Box.createHorizontalGlue());
        contentPanel.add(checkboxWrapper);

        contentPanel.add(Box.createVerticalStrut(20));

        //--------------------------------------------------------------------------------------------------------------
        // Submit Button and Display Textbox
        //--------------------------------------------------------------------------------------------------------------
        JButton submitBtn = new JButton("Submit Changes");
        submitBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea displayTextArea = new JTextArea(12, 40);
        displayTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        displayTextArea.setEditable(false);
        displayTextArea.setLineWrap(true);
        displayTextArea.setWrapStyleWord(true);
        displayTextArea.setBackground(Color.WHITE);
        displayTextArea.setBorder(new LineBorder(Color.GRAY, 1));

        JScrollPane scrollPane = new JScrollPane(displayTextArea);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(400, 200));

        //----------------------------------------------------------------------------------------------------------
        // Macronutrients Section (grams/day) - integer inputs only
        //----------------------------------------------------------------------------------------------------------
        JLabel macrosLabel = new JLabel("Macronutrients (g/day):");
        macrosLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        macrosLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(macrosLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel macrosPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        macrosPanel.setBackground(LIGHT_GRAY);

        JLabel carbsLabel = new JLabel("Carbs:");
        JTextField carbsField = new JTextField(6);
        carbsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        carbsField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel proteinLabel = new JLabel("Protein:");
        JTextField proteinField = new JTextField(6);
        proteinLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        proteinField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel fatLabel = new JLabel("Fat:");
        JTextField fatField = new JTextField(6);
        fatLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fatField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        macrosPanel.add(carbsLabel);
        macrosPanel.add(carbsField);
        macrosPanel.add(proteinLabel);
        macrosPanel.add(proteinField);
        macrosPanel.add(fatLabel);
        macrosPanel.add(fatField);

        contentPanel.add(macrosPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Helper lambda to display combined output
        Runnable displayCombinedOutput = () -> {
            StringBuilder combinedOutput = new StringBuilder();

            // Get dietary restrictions
            StringBuilder selectedRestrictions = new StringBuilder();
            for (JCheckBox checkbox : dietaryCheckboxes) {
                if (checkbox.isSelected()) {
                    if (selectedRestrictions.length() > 0) {
                        selectedRestrictions.append("\n");
                    }
                    selectedRestrictions.append("• ").append(checkbox.getText());
                }
            }

            // Get macronutrients
            try {
                String carbsText = carbsField.getText().trim();
                String proteinText = proteinField.getText().trim();
                String fatText = fatField.getText().trim();

                if (!carbsText.isEmpty() && !proteinText.isEmpty() && !fatText.isEmpty()) {
                    int carbs = Integer.parseInt(carbsText);
                    int protein = Integer.parseInt(proteinText);
                    int fat = Integer.parseInt(fatText);

                    if (selectedRestrictions.length() > 0) {
                        combinedOutput.append("Dietary Restrictions:\n").append(selectedRestrictions).append("\n\n");
                    }

                    combinedOutput.append("Macronutrients (g/day):\n");
                    combinedOutput.append("Carbs: ").append(carbs).append("\n");
                    combinedOutput.append("Protein: ").append(protein).append("\n");
                    combinedOutput.append("Fat: ").append(fat);
                } else if (selectedRestrictions.length() > 0) {
                    combinedOutput.append("Dietary Restrictions:\n").append(selectedRestrictions);
                } else {
                    combinedOutput.append("No dietary restrictions or macronutrients entered.");
                }
            } catch (NumberFormatException nfe) {
                if (selectedRestrictions.length() > 0) {
                    combinedOutput.append("Dietary Restrictions:\n").append(selectedRestrictions).append("\n\n");
                    combinedOutput.append("⚠ Macronutrients: Please enter integer values (whole grams).");
                } else {
                    combinedOutput.append("⚠ Please enter integer values (whole grams) for macronutrients.");
                }
            }

            displayTextArea.setText(combinedOutput.toString());
        };

        submitBtn.addActionListener(e -> displayCombinedOutput.run());

        contentPanel.add(submitBtn);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(scrollPane);

        // Add glue at the bottom to prevent stretching
        contentPanel.add(Box.createVerticalGlue());

        // Set up the page as a scrollable panel
        setLayout(new BorderLayout());
        JScrollPane pageScrollPane = new JScrollPane(contentPanel);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScrollPane, BorderLayout.CENTER);
    }

    /*
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
    */
}

