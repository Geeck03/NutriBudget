package pages;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.Timer;

import org.jfree.chart.util.StringUtils;

public class Page2 extends JPanel {

    //==============================================================================================================
    // Ingredient class
    //==============================================================================================================
    public static class Ingredient {
        public int id;
        public String name;
        public double cost;
        public int calories;
        public int protein;
        public int carbs;
        public double fat;
        public String description;
        public String imagePath;

        public Ingredient(int id, String name, double cost, int calories, int protein, int carbs, double fat, String description, String imagePath) {
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
        List<Ingredient> ingredients;
    List<Recipe> recipes;


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
    private final Set<Integer> favoriteIngredientIds = new HashSet<>();
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();
    private final JComboBox<String> filterDropdown = new JComboBox<>(new String[] {
        "All", "A+", "A", "B", "C", "D", "E"
    });



    private final List<Ingredient> ingredients;
    private final List<Recipe> recipes;

    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private JPanel sidebarPanel;
    private JSplitPane splitPane;
    private boolean sidebarVisible = false;
    private Timer sidebarTimer;
    private int targetDividerLocation;
    private final int sidebarWidth = 480;

    private boolean showingFavorites = false;
    private boolean showingIngredients = true;

    private final JButton allButton = new JButton("All");
    private final JButton favoritesButton = new JButton("Favorites");
    private final JTextField searchField = new JTextField(20);

    private final JTabbedPane mainTabs = new JTabbedPane();

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public Page2() {
        setLayout(new BorderLayout());
        ingredients = loadIngredients("text/ingredients.txt");
        recipes = loadRecipes("text/recipes.txt");
        buildUI();

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(getWidth());
            sidebarVisible = false;
        });
    }

    //==============================================================================================================
    // UI construction
    //==============================================================================================================
    private void buildUI() {
        // Top bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));


        // Search bar
        searchField.setPreferredSize(new Dimension(250, 28));
        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        // Dynamic search on typing
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refreshGrid();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refreshGrid();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refreshGrid();
            }
        });


        // All / Favorites buttons
        allButton.addActionListener(e -> showAll());
        favoritesButton.addActionListener(e -> showFavorites());

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(allButton);
        topPanel.add(favoritesButton);

        add(topPanel, BorderLayout.NORTH);

        // Tabs
        mainTabs.addTab("Ingredients", null);
        mainTabs.addTab("Recipes", null);
        mainTabs.addChangeListener(e -> {
            showingIngredients = mainTabs.getSelectedIndex() == 0;
            showAll();
        });

        add(mainTabs, BorderLayout.SOUTH);

        // Grid + sidebar
        gridPanel = buildGridPanelForIngredients(ingredients);
        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, 600));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, sidebarPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);

        add(splitPane, BorderLayout.CENTER);

        topPanel.add(new JLabel("NutriScore:"));
        filterDropdown.setPreferredSize(new Dimension(120, 28));
        filterDropdown.addActionListener(e -> refreshGrid()); // dynamically refresh on selection
        topPanel.add(filterDropdown);
    }

    //==============================================================================================================
    // Grid builder
    //==============================================================================================================
    private JPanel buildGridPanelForIngredients(List<Ingredient> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);

        // All items are already filtered by smartSearch in refreshGrid
        for (Ingredient ing : items) {
            grid.add(createIngredientCard(ing));
        }

        return grid;
    }

    private JPanel buildGridPanelForRecipes(List<Recipe> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);

        // All items are already filtered by smartSearch in refreshGrid
        for (Recipe r : items) {
            grid.add(createRecipeCard(r));
        }

        return grid;
    }


    //==============================================================================================================
    // Card creation
    //==============================================================================================================
    private JPanel createIngredientCard(Ingredient ing) {
        JPanel card = baseCard(ing.name, ing.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                showSidebar(ing);
            }
        });
        return card;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = baseCard(r.name, r.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
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

        ImageIcon icon = loadImageIcon("images/" + imagePath, 120, 90);
        if (icon != null) image.setIcon(icon);
        else {
            image.setText("[No Image]");
            image.setForeground(Color.GRAY);
        }

        card.add(image);
        card.add(Box.createVerticalStrut(8));
        card.add(name);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(230, 240, 255));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(250, 250, 250));
            }
        });

        return card;
    }

    //==============================================================================================================
    // Sidebar creation and behavior
    //==============================================================================================================
    private JPanel createSidebarContent(Ingredient ing) {
        return sidebarTemplate(
                ing.name, ing.imagePath,
                String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %d<br>Carbs: %d<br>Fat: %.1f</html>",
                        ing.cost, ing.calories, ing.protein, ing.carbs, ing.fat),
                ing.description, ing.id, true
        );
    }

    private JPanel createSidebarContent(Recipe r) {
        return sidebarTemplate(
                r.name, r.imagePath,
                String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %d<br>Carbs: %d<br>Fat: %.1f</html>",
                        r.cost, r.calories, r.protein, r.carbs, r.fat),
                r.description, r.id, false
        );
    }

    private JPanel sidebarTemplate(String name, String imagePath, String info, String desc, int id, boolean isIngredient) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton exitButton = new JButton("✖");
        exitButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        exitButton.setFocusPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorderPainted(false);
        exitButton.setForeground(Color.DARK_GRAY);
        exitButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        exitButton.addActionListener(e -> hideSidebar());
        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                exitButton.setForeground(Color.RED);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                exitButton.setForeground(Color.DARK_GRAY);
            }
        });

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int sidebarImageWidth = sidebarWidth - 80;
        int sidebarImageHeight = (int) (sidebarImageWidth * 0.6);
        JLabel imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon = loadImageIcon("images/" + imagePath, sidebarImageWidth, sidebarImageHeight);
        if (icon != null) imageLabel.setIcon(icon);
        else imageLabel.setText("[No Image]");

        JLabel infoLabel = new JLabel(info);
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JTextArea descArea = new JTextArea(desc == null || desc.isEmpty() ? "No description available." : desc);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(new Color(250, 250, 250));
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton favoriteButton = new JButton(isIngredient
                ? (isFavoriteIngredient(id) ? "★ Favorite" : "☆ Favorite")
                : (isFavoriteRecipe(id) ? "★ Favorite" : "☆ Favorite"));
        favoriteButton.addActionListener(e -> {
            if (isIngredient) toggleFavoriteIngredient(id, favoriteButton);
            else toggleFavoriteRecipe(id, favoriteButton);
        });

        panel.add(exitButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(imageLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(infoLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JScrollPane(descArea));
        panel.add(Box.createVerticalStrut(10));
        panel.add(favoriteButton);

        return panel;
    }

    private void showSidebar(Ingredient ing) {
        sidebarPanel.removeAll();
        sidebarPanel.add(createSidebarContent(ing), BorderLayout.CENTER);
        animateSidebar(true);
    }

    private void showSidebar(Recipe r) {
        sidebarPanel.removeAll();
        sidebarPanel.add(createSidebarContent(r), BorderLayout.CENTER);
        animateSidebar(true);
    }

    private void hideSidebar() {
        animateSidebar(false);
    }

    private void animateSidebar(boolean show) {
        if (sidebarTimer != null && sidebarTimer.isRunning()) sidebarTimer.stop();

        final int[] current = {splitPane.getDividerLocation()};
        targetDividerLocation = show ? getWidth() - sidebarWidth : getWidth();

        sidebarTimer = new Timer(10, e -> {
            int diff = targetDividerLocation - current[0];
            if (Math.abs(diff) < 4) {
                splitPane.setDividerLocation(targetDividerLocation);
                sidebarTimer.stop();
                if (!show) sidebarPanel.removeAll();
            } else {
                current[0] += diff / 5;
                splitPane.setDividerLocation(current[0]);
            }
        });
        sidebarTimer.start();

        sidebarVisible = show;
    }

    //==============================================================================================================
    // Favorites
    //==============================================================================================================
    private boolean isFavoriteIngredient(int id) {
        return favoriteIngredientIds.contains(id);
    }

    private boolean isFavoriteRecipe(int id) {
        return favoriteRecipeIds.contains(id);
    }

    private void toggleFavoriteIngredient(int id, JButton button) {
        if (isFavoriteIngredient(id)) {
            favoriteIngredientIds.remove(id);
            button.setText("☆ Favorite");
        } else {
            favoriteIngredientIds.add(id);
            button.setText("★ Favorite");
        }
        refreshGrid();
    }

    private void toggleFavoriteRecipe(int id, JButton button) {
        if (isFavoriteRecipe(id)) {
            favoriteRecipeIds.remove(id);
            button.setText("☆ Favorite");
        } else {
            favoriteRecipeIds.add(id);
            button.setText("★ Favorite");
        }
        refreshGrid();
    }

    private void showFavorites() {
        showingFavorites = true;
        refreshGrid();
    }

    private void showAll() {
        showingFavorites = false;
        refreshGrid();
    }

    private void refreshGrid() {
        String query = searchField.getText().trim();

        if (showingIngredients) {
            // Start with either favorites or all ingredients
            List<Ingredient> list = showingFavorites
                    ? ingredients.stream().filter(i -> favoriteIngredientIds.contains(i.id)).toList()
                    : new ArrayList<>(ingredients);

            if (!query.isEmpty()) {
                // Convert Ingredient objects to strings for searching
                List<String> names = list.stream().map(i -> i.name).toList();
                List<String> matchedNames = SearchAlgorithms.smartSearch(query, names);

                // Filter original list based on matched names
                Set<String> matchedSet = new HashSet<>(matchedNames); // no scores
                list = list.stream().filter(i -> matchedSet.contains(i.name)).toList();
            }

            gridPanel.removeAll();
            gridPanel = buildGridPanelForIngredients(list);

        } else {
            // Start with either favorites or all recipes
            List<Recipe> list = showingFavorites
                    ? recipes.stream().filter(r -> favoriteRecipeIds.contains(r.id)).toList()
                    : new ArrayList<>(recipes);

            if (!query.isEmpty()) {
                List<String> names = list.stream().map(r -> r.name).toList();
                List<String> matchedNames = SearchAlgorithms.smartSearch(query, names);

                Set<String> matchedSet = new HashSet<>(matchedNames); // no scores
                list = list.stream().filter(r -> matchedSet.contains(r.name)).toList();
            }

            gridPanel.removeAll();
            gridPanel = buildGridPanelForRecipes(list);
        }

        scrollPane.setViewportView(gridPanel);
        revalidate();
        repaint();
    }


    //==============================================================================================================
    // File loading (from src/pages/text/)
    //==============================================================================================================
    private List<Ingredient> loadIngredients(String filePath) {
        List<Ingredient> list = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(filePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line = br.readLine();
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
                    list.add(new Ingredient(id, name, cost, calories, protein, carbs, fat, desc, img));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading ingredients from: " + filePath);
            e.printStackTrace();
        }
        return list;
    }

    private List<Recipe> loadRecipes(String filePath) {
        List<Recipe> list = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(filePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line = br.readLine();
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
        } catch (Exception e) {
            System.err.println("Error loading recipes from: " + filePath);
            e.printStackTrace();
        }
        return list;
    }

    private double safeParseDouble(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0.0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int safeParseInt(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ImageIcon loadImageIcon(String path, int width, int height) {
        try {
            java.net.URL resource = getClass().getResource(path);
            if (resource == null) {
                System.err.println("Image not found: " + path);
                return null;
            }
            ImageIcon icon = new ImageIcon(resource);
            Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path);
            return null;
        }
    }

    //==============================================================================================================
    // Test main
    //==============================================================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ingredients & Recipes");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 700);
            frame.add(new Page2());
            frame.setVisible(true);
        });
    }
}
