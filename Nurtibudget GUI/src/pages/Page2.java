package pages;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import py4j.ClientServer;
import py4j.Py4JNetworkException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.Timer;

import org.jfree.chart.util.StringUtils;

import bridge.IKrogerWrapper;

//======================================================================================================================
// Page2 - Ingredients & Recipes with Py4J integration (Kroger API)
//======================================================================================================================
public class Page2 extends JPanel {

    //==============================================================================================================
    // Ingredient class
    //==============================================================================================================
    public static class Ingredient {
        public String name;
        public String info;
        public String imagePath;
        public List<String> nutrients;

        public Ingredient(String name, String info, String imagePath) {
            this.name = name;
            this.info = info;
            this.imagePath = imagePath;
            this.nutrients = new ArrayList<>();
        public int id;
        public String name;
        public double cost;
        public int calories;
        public double protein;
        public double carbs;
        public double fat;
        public String description;
        public String imagePath;
        public String nutriGrade;

        public double vitaminA, vitaminB1, vitaminB2, vitaminB3, vitaminB5, vitaminB6, vitaminB7,
                    vitaminB9, vitaminB12, vitaminC, vitaminD, vitaminE, vitaminK;
        public double calcium, phosphorus, magnesium, sodium, potassium, chloride, sulfur,
                    iron, zinc, copper, manganese, iodine, selenium, molybdenum, chromium,
                    fluoride, cobalt, fiber;

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

        public Recipe(int id, String name, double cost, int calories, int protein, int carbs,
                      double fat, String description, String imagePath) {
        public double protein;
        public double carbs;
        public double fat;
        public String description;
        public String imagePath;
        public String nutriGrade;


        public double vitaminA, vitaminB1, vitaminB2, vitaminB3, vitaminB5, vitaminB6, vitaminB7,
                    vitaminB9, vitaminB12, vitaminC, vitaminD, vitaminE, vitaminK;
        public double calcium, phosphorus, magnesium, sodium, potassium, chloride, sulfur,
                    iron, zinc, copper, manganese, iodine, selenium, molybdenum, chromium,
                    fluoride, cobalt, fiber;


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
    private final Set<String> favoriteIngredientNames = new HashSet<>();
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();

    private List<Ingredient> ingredients;
    private final List<Recipe> recipes;
    private final Set<Integer> favoriteIngredientIds = new HashSet<>();
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();
    private final JComboBox<String> filterDropdown = new JComboBox<>(new String[] {
        "All", "A", "B", "C", "D", "E"
    });



    private final List<Ingredient> ingredients;
    private final List<Recipe> recipes;
    // Dietary filters
    private boolean filterVegetarian = false;
    private boolean filterVegan = false;
    private boolean filterGlutenFree = false;
    

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

    private ClientServer clientServer;
    private IKrogerWrapper pythonEntry;

    private final Path favoritesDir = Paths.get("src/pages/favorites");

    private final JTabbedPane mainTabs = new JTabbedPane();

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public Page2() {
        setLayout(new BorderLayout());
        try { Files.createDirectories(favoritesDir); } catch (IOException e) { e.printStackTrace(); }

        initPythonConnection();

        ingredients = loadIngredientsFromPython("");
        recipes = loadRecipes("text/recipes.txt");
        loadSavedFavorites();
        ingredients = ingredientLoader.loadIngredients("text/ingredients.txt");
        recipes = recipeLoader.loadRecipes("text/recipes.txt");


        buildUI();

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(getWidth());
            sidebarVisible = false;
        });
    }

    //==============================================================================================================
    // Initialize Python connection
    //==============================================================================================================
    private void initPythonConnection() {
        try {
            System.out.println("🔌 Connecting to Python on port 25333...");
            clientServer = new ClientServer(null);
            pythonEntry = (IKrogerWrapper) clientServer.getPythonServerEntryPoint(
                    new Class[]{IKrogerWrapper.class}
            );
            System.out.println("✅ Connected to Python!");
        } catch (Py4JNetworkException e) {
            System.err.println("❌ Could not connect to Python server!");
            e.printStackTrace();
        }
    }

    //==============================================================================================================
    // Build UI
    //==============================================================================================================
    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchField.setPreferredSize(new Dimension(250, 28));
        searchField.addActionListener(e -> refreshGrid());
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


        add(topPanel, BorderLayout.NORTH);

        // Tabs
        mainTabs.addTab("Ingredients", null);
        mainTabs.addTab("Recipes", null);
        mainTabs.addChangeListener(e -> {
            showingIngredients = mainTabs.getSelectedIndex() == 0;
            showAll();
        });
        add(mainTabs, BorderLayout.SOUTH);

        gridPanel = new JPanel();
        gridPanel.setBackground(Color.WHITE);

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
    }

    //==============================================================================================================
    // Grid panels

        add(splitPane, BorderLayout.CENTER);

        topPanel.add(new JLabel("NutriScore:"));
        filterDropdown.setPreferredSize(new Dimension(120, 28));
        filterDropdown.addActionListener(e -> refreshGrid()); // dynamically refresh on selection
        topPanel.add(filterDropdown);

        // Dietary filter dropdown (as a popup menu)
        JButton dietFilterButton = new JButton("Dietary Restrictions ▼");

        JPopupMenu dietMenu = new JPopupMenu();

        // Create checkbox menu items
        JCheckBoxMenuItem vegetarianItem = new JCheckBoxMenuItem("Vegetarian");
        JCheckBoxMenuItem veganItem = new JCheckBoxMenuItem("Vegan");
        JCheckBoxMenuItem glutenFreeItem = new JCheckBoxMenuItem("Gluten-Free");

        // Add listeners to each filter option
        vegetarianItem.addActionListener(e -> {
            filterVegetarian = vegetarianItem.isSelected();
            refreshGrid();
        });

        veganItem.addActionListener(e -> {
            filterVegan = veganItem.isSelected();
            refreshGrid();
        });

        glutenFreeItem.addActionListener(e -> {
            filterGlutenFree = glutenFreeItem.isSelected();
            refreshGrid();
        });

        // Add them to the popup menu
        dietMenu.add(vegetarianItem);
        dietMenu.add(veganItem);
        dietMenu.add(glutenFreeItem);

        // Show menu when button is clicked
        dietFilterButton.addActionListener(e ->
            dietMenu.show(dietFilterButton, 0, dietFilterButton.getHeight())
        );

        // Add to top panel
        topPanel.add(dietFilterButton);

    }

    //==============================================================================================================
    // Grid builder
    //==============================================================================================================
    private JPanel buildGridPanelForIngredients(List<Ingredient> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Ingredient ing : items)
            if (matchesSearch(ing.name)) grid.add(createIngredientCard(ing));

        // All items are already filtered by smartSearch in refreshGrid
        for (Ingredient ing : items) {
            grid.add(createIngredientCard(ing));
        }

        return grid;
    }

    private JPanel buildGridPanelForRecipes(List<Recipe> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Recipe r : items)
            if (matchesSearch(r.name)) grid.add(createRecipeCard(r));
        return grid;
    }

    //==============================================================================================================
    // Cards

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
            @Override public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(ing); }
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
            @Override public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(r); }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                showSidebar(r);
            }
        });
        return card;
    }

    private JPanel baseCard(String nameText, String imageUrl) {
    private JPanel baseCard(String nameText, String imagePath) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(250, 250, 250));
        card.setPreferredSize(new Dimension(180, 180));

        JLabel name = new JLabel(nameText, SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel image = new JLabel();
        image.setAlignmentX(Component.CENTER_ALIGNMENT);
        image.setPreferredSize(new Dimension(120, 90));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(new URL(imageUrl));
                Image scaled = icon.getImage().getScaledInstance(120, 90, Image.SCALE_SMOOTH);
                image.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                image.setText("[No Image]");
                image.setForeground(Color.GRAY);
            }
        } else {
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
    // Sidebar
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

    private void animateSidebar(boolean show) {
        if (sidebarTimer != null && sidebarTimer.isRunning()) sidebarTimer.stop();
        final int[] current = {splitPane.getDividerLocation()};
        int target = show ? getWidth() - sidebarWidth : getWidth();
        sidebarTimer = new Timer(10, e -> {
            int diff = target - current[0];
            if (Math.abs(diff) < 4) {
                splitPane.setDividerLocation(target);
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

    private JPanel createSidebarContent(Ingredient ing) {
        return sidebarTemplate(ing.name, ing.imagePath, ing.info, ing.nutrients, ing.name, true);
    }

    private JPanel createSidebarContent(Recipe r) {
        return sidebarTemplate(r.name, r.imagePath, r.description, null, String.valueOf(r.id), false);
    }

    private JPanel sidebarTemplate(String name, String img, String info, List<String> nutrients, String idOrName, boolean ingredient) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JButton close = new JButton("✖");
        close.addActionListener(e -> animateSidebar(false));

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (img != null && !img.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(new URL(img));
                Image scaled = icon.getImage().getScaledInstance(sidebarWidth - 80, 250, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                imageLabel.setText("[No Image]");
            }
        } else {
            imageLabel.setText("[No Image]");
        }

        JTextArea infoArea = new JTextArea(info);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);

        JTextArea nutrientArea = new JTextArea();
        if (nutrients != null && !nutrients.isEmpty()) {
            nutrientArea.setText(String.join("\n", nutrients));
        }
        nutrientArea.setEditable(false);

        JButton fav = new JButton(ingredient ?
                (favoriteIngredientNames.contains(idOrName) ? "★ Favorite" : "☆ Favorite")
                : (favoriteRecipeIds.contains(Integer.parseInt(idOrName)) ? "★ Favorite" : "☆ Favorite"));
        fav.addActionListener(e -> {
            if (ingredient) toggleFavoriteIngredient(idOrName, fav);
            else toggleFavoriteRecipe(Integer.parseInt(idOrName), fav);
        });

        panel.add(close);
        panel.add(Box.createVerticalStrut(10));
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(imageLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JScrollPane(infoArea));
        if (ingredient) panel.add(new JScrollPane(nutrientArea));
        panel.add(Box.createVerticalStrut(10));
        panel.add(fav);
        return panel;
    }

    //==============================================================================================================
    // Favorites / Filtering
    //==============================================================================================================
    private void toggleFavoriteIngredient(String name, JButton b) {
        if (favoriteIngredientNames.remove(name)) {
            b.setText("☆ Favorite");
            removeFavoriteFile(name);
        } else {
            favoriteIngredientNames.add(name);
            b.setText("★ Favorite");
            saveFavoriteFile(name);

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

    private void toggleFavoriteRecipe(int id, JButton b) {
        if (favoriteRecipeIds.remove(id)) b.setText("☆ Favorite");
        else { favoriteRecipeIds.add(id); b.setText("★ Favorite"); }
        refreshGrid();
    }

    private void showFavorites() { showingFavorites = true; refreshGrid(); }
    private void showAll() { showingFavorites = false; refreshGrid(); }

    private void refreshGrid() {
        gridPanel.removeAll();

        // Show loading
        gridPanel.setLayout(new BorderLayout());
        JLabel loading = new JLabel("Loading...", SwingConstants.CENTER);
        loading.setFont(new Font("SansSerif", Font.BOLD, 24));
        gridPanel.add(loading, BorderLayout.CENTER);
        gridPanel.revalidate();
        gridPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            List<Ingredient> list;
            String query = searchField.getText().trim();

            if (showingIngredients) {
                if (query.isEmpty() && showingFavorites) {
                    list = loadSavedFavorites();
                } else {
                    list = showingFavorites ?
                            ingredients.stream().filter(i -> favoriteIngredientNames.contains(i.name)).toList()
                            : loadIngredientsFromPython(query);
                }
                gridPanel = buildGridPanelForIngredients(list);
            } else {
                List<Recipe> listR = showingFavorites ?
                        recipes.stream().filter(r -> favoriteRecipeIds.contains(r.id)).toList()
                        : recipes;
                gridPanel = buildGridPanelForRecipes(listR);
            }

            scrollPane.setViewportView(gridPanel);
            revalidate();
            repaint();
        });
    }

    private boolean matchesSearch(String name) {
        String q = searchField.getText().trim().toLowerCase();
        return q.isEmpty() || name.toLowerCase().contains(q);
    }

    //==============================================================================================================
    // Data loading
    //==============================================================================================================
    private List<Ingredient> loadIngredientsFromPython(String query) {
        List<Ingredient> list = new ArrayList<>();
        if (pythonEntry == null) return list;

        try {
            String jsonString = pythonEntry.search(query, 10);
            JSONArray array = new JSONArray(jsonString);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                Ingredient ing = new Ingredient(
                        obj.optString("name", "Unknown"),
                        obj.optString("info", obj.optString("describe", "")),
                        obj.optString("image_url", "")
                );

                JSONArray nutArray = obj.optJSONArray("nutrients");
                if (nutArray != null) {
                    for (int j = 0; j < nutArray.length(); j++)
                        ing.nutrients.add(nutArray.getString(j));
                }

                list.add(ing);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private List<Recipe> loadRecipes(String path) {
        List<Recipe> list = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t");
                if (p.length >= 7) {
                    list.add(new Recipe(
                            Integer.parseInt(p[0]), p[1], Double.parseDouble(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                            Integer.parseInt(p[5]), Double.parseDouble(p[6]),
                            p.length > 7 ? p[7] : "", p.length > 8 ? p[8] : ""));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    //==============================================================================================================
    // Favorite file handling
    //==============================================================================================================
    private void saveFavoriteFile(String name) {
        try {
            Path file = favoritesDir.resolve(name + ".json");
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            for (Ingredient ing : ingredients) {
                if (ing.name.equals(name)) {
                    obj.put("info", ing.info);
                    obj.put("image", ing.imagePath);
                    obj.put("nutrients", ing.nutrients);
                }
            }
            Files.write(file, obj.toString().getBytes());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void removeFavoriteFile(String name) {
        try { Files.deleteIfExists(favoritesDir.resolve(name + ".json")); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private List<Ingredient> loadSavedFavorites() {
        List<Ingredient> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(favoritesDir, "*.json")) {
            for (Path path : stream) {
                String content = Files.readString(path);
                JSONObject obj = new JSONObject(content);
                Ingredient ing = new Ingredient(
                        obj.optString("name"),
                        obj.optString("info"),
                        obj.optString("image")
                );
                JSONArray nutArray = obj.optJSONArray("nutrients");
                if (nutArray != null) {
                    for (int j = 0; j < nutArray.length(); j++) ing.nutrients.add(nutArray.getString(j));
                }
                list.add(ing);
                favoriteIngredientNames.add(ing.name);
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
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

            String selectedGrade = (String) filterDropdown.getSelectedItem();
            if (!"All".equals(selectedGrade)) {
                list = list.stream()
                        .filter(i -> selectedGrade.equalsIgnoreCase(i.nutriGrade))
                        .collect(Collectors.toList());
            }

            // Apply dietary filters
            if (filterVegetarian) {
                list = list.stream()
                        .filter(i -> i.description != null && i.description.toLowerCase().contains("vegetarian"))
                        .collect(Collectors.toList());
            }

            if (filterVegan) {
                list = list.stream()
                        .filter(i -> i.description != null && i.description.toLowerCase().contains("vegan"))
                        .collect(Collectors.toList());
            }

            if (filterGlutenFree) {
                list = list.stream()
                        .filter(i -> i.description != null && i.description.toLowerCase().contains("gluten-free"))
                        .collect(Collectors.toList());
            }

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

            String selectedGrade = (String) filterDropdown.getSelectedItem();
            if (!"All".equals(selectedGrade)) {
                list = list.stream()
                        .filter(r -> selectedGrade.equalsIgnoreCase(r.nutriGrade))
                        .collect(Collectors.toList());
            }


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
