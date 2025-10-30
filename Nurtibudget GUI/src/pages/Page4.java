package pages;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Page4 extends JPanel {

    //==============================================================================================================
    // Recipe class
    //==============================================================================================================
    public static class Recipe {
        public int id;
        public String name;
        public double cost;
        public int calories;
        public int protein;
        public int carbs;
        public double fat;
        public String description;
        public String imagePath;

        public Recipe(int id, String name, double cost, int calories, int protein, int carbs, double fat, String description, String imagePath) {
            this.id = id;
            this.name = name;
            this.cost = cost;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.description = description;
            this.imagePath = imagePath;
        }
    }

    //==============================================================================================================
    // Instance fields
    //==============================================================================================================
    private final List<Recipe> recipes;
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();

    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private JTextField searchField = new JTextField(20);

    private final JButton allButton = new JButton("All");
    private final JButton favoritesButton = new JButton("Favorites");
    private boolean showingFavorites = false;

    private static final String CUSTOM_RECIPE_FILE = "src/pages/text/custom_recipes.txt";

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public Page4() {
        recipes = loadRecipes(CUSTOM_RECIPE_FILE);
        setLayout(new BorderLayout());
        buildUI();
    }

    //==============================================================================================================
    // UI Construction
    //==============================================================================================================
    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        searchField.setPreferredSize(new Dimension(250, 28));
        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        searchField.addActionListener(e -> refreshGrid());

        allButton.addActionListener(e -> showAll());
        favoritesButton.addActionListener(e -> showFavorites());

        JButton addButton = new JButton("+ Add Recipe");
        addButton.addActionListener(e -> openAddRecipeDialog());

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(allButton);
        topPanel.add(favoritesButton);
        topPanel.add(addButton);

        add(topPanel, BorderLayout.NORTH);

        gridPanel = buildGridPanel(recipes);
        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    //==============================================================================================================
    // Grid Builder
    //==============================================================================================================
    private JPanel buildGridPanel(List<Recipe> list) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);

        for (Recipe r : list) {
            if (matchesSearch(r.name))
                grid.add(createRecipeCard(r));
        }

        return grid;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = baseCard(r.name, r.imagePath);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showSidebar(r);
            }
        });
        return card;
    }

    private JPanel baseCard(String nameText, String imagePath) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(250, 250, 250));
        card.setPreferredSize(new Dimension(180, 180));

        JLabel name = new JLabel(nameText, SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel image = new JLabel();
        image.setAlignmentX(Component.CENTER_ALIGNMENT);
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(120, 90));

        ImageIcon icon = loadImageIcon(imagePath, 120, 90);
        if (icon != null) image.setIcon(icon);
        else {
            image.setText("[No Image]");
            image.setForeground(Color.GRAY);
        }

        card.add(image);
        card.add(Box.createVerticalStrut(8));
        card.add(name);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(230, 240, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(250, 250, 250));
            }
        });

        return card;
    }

    //==============================================================================================================
    // Sidebar
    //==============================================================================================================
    private void showSidebar(Recipe r) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), r.name, true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(r.name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = loadImageIcon(r.imagePath, 300, 180);
        if (icon != null) imageLabel.setIcon(icon);
        else imageLabel.setText("[No Image]");

        JLabel infoLabel = new JLabel(String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %d<br>Carbs: %d<br>Fat: %.1f</html>",
                r.cost, r.calories, r.protein, r.carbs, r.fat));

        JTextArea descArea = new JTextArea(r.description != null ? r.description : "No description available.");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(new Color(250, 250, 250));
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton deleteButton = new JButton("Delete Recipe");
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to delete this recipe?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                recipes.remove(r);
                saveRecipesToFile();
                refreshGrid();
                dialog.dispose();
            }
        });

        dialog.add(Box.createVerticalStrut(10));
        dialog.add(nameLabel);
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(imageLabel);
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(infoLabel);
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(new JScrollPane(descArea));
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(deleteButton);

        dialog.setVisible(true);
    }

    //==============================================================================================================
    // Add Recipe Dialog
    //==============================================================================================================
    private void openAddRecipeDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Recipe", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

        JTextField nameField = new JTextField();
        JTextField costField = new JTextField();
        JTextField caloriesField = new JTextField();
        JTextField proteinField = new JTextField();
        JTextField carbsField = new JTextField();
        JTextField fatField = new JTextField();
        JTextArea descArea = new JTextArea(5, 20);

        JButton selectImageButton = new JButton("Select Image");
        final String[] selectedImagePath = {null};
        selectImageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String copiedPath = copyImageToImagesFolder(file);
                if (copiedPath != null) selectedImagePath[0] = copiedPath;
            }
        });

        dialog.add(new JLabel("Name:")); dialog.add(nameField);
        dialog.add(new JLabel("Cost:")); dialog.add(costField);
        dialog.add(new JLabel("Calories:")); dialog.add(caloriesField);
        dialog.add(new JLabel("Protein:")); dialog.add(proteinField);
        dialog.add(new JLabel("Carbs:")); dialog.add(carbsField);
        dialog.add(new JLabel("Fat:")); dialog.add(fatField);
        dialog.add(new JLabel("Description:")); dialog.add(new JScrollPane(descArea));
        dialog.add(selectImageButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            int nextId = recipes.stream().mapToInt(r -> r.id).max().orElse(0) + 1;
            Recipe r = new Recipe(
                    nextId,
                    nameField.getText(),
                    safeParseDouble(costField.getText()),
                    safeParseInt(caloriesField.getText()),
                    safeParseInt(proteinField.getText()),
                    safeParseInt(carbsField.getText()),
                    safeParseDouble(fatField.getText()),
                    descArea.getText(),
                    selectedImagePath[0]
            );
            recipes.add(r);
            saveRecipesToFile();
            refreshGrid();
            dialog.dispose();
        });

        dialog.add(saveButton);
        dialog.setVisible(true);
    }

    //==============================================================================================================
    // Copy Image Helper
    //==============================================================================================================
    private String copyImageToImagesFolder(File sourceFile) {
        try {
            File imagesDir = new File("src/pages/images");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            File destFile = new File(imagesDir, sourceFile.getName());
            int count = 1;
            String name = sourceFile.getName();
            String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            while (destFile.exists()) {
                destFile = new File(imagesDir, baseName + "_" + count + ext);
                count++;
            }

            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath());
            return destFile.getAbsolutePath(); // ✅ Use absolute path
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to copy image: " + e.getMessage());
            return null;
        }
    }

    //==============================================================================================================
    // File loading/saving
    //==============================================================================================================
    private List<Recipe> loadRecipes(String filePath) {
        List<Recipe> list = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t");
                if (p.length >= 7) {
                    int id = safeParseInt(p[0]);
                    String name = p[1];
                    double cost = safeParseDouble(p[2]);
                    int calories = safeParseInt(p[3]);
                    int protein = safeParseInt(p[4]);
                    int carbs = safeParseInt(p[5]);
                    double fat = safeParseDouble(p[6]);
                    String desc = p.length > 7 ? p[7] : "";
                    String img = p.length > 8 ? p[8] : "";
                    list.add(new Recipe(id, name, cost, calories, protein, carbs, fat, desc, img));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveRecipesToFile() {
        try {
            File file = new File(CUSTOM_RECIPE_FILE);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("id\tname\tcost\tcalories\tprotein\tcarbs\tfat\tdescription\timagePath\n");
                for (Recipe r : recipes) {
                    fw.write(String.format("%d\t%s\t%.2f\t%d\t%d\t%d\t%.1f\t%s\t%s\n",
                            r.id, r.name, r.cost, r.calories, r.protein, r.carbs, r.fat,
                            r.description != null ? r.description.replace("\n", " ") : "",
                            r.imagePath != null ? r.imagePath : ""));
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save recipes: " + e.getMessage());
        }
    }

    //==============================================================================================================
    // Helpers
    //==============================================================================================================
    private boolean matchesSearch(String name) {
        String term = searchField.getText().trim().toLowerCase();
        return term.isEmpty() || name.toLowerCase().contains(term);
    }

    private void refreshGrid() {
        remove(scrollPane);
        List<Recipe> toShow = showingFavorites
                ? recipes.stream().filter(r -> favoriteRecipeIds.contains(r.id)).toList()
                : recipes;
        gridPanel = buildGridPanel(toShow);
        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showAll() {
        showingFavorites = false;
        refreshGrid();
    }

    private void showFavorites() {
        showingFavorites = true;
        refreshGrid();
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private double safeParseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private ImageIcon loadImageIcon(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
