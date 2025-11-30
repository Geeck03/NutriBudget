package pages;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;


//======================================================================================================================
// Page2 - Ingredients & Recipes with DB integration
//======================================================================================================================
public class Page2 extends JPanel {

    

    
    //==============================================================================================================
    // Listener used when Page2 is used as an ingredient selector
    //==============================================================================================================
    public interface IngredientSelectionListener {
        void ingredientSelected(JSONObject selection);
    }

    private IngredientSelectionListener selectionListener = null;

    //==============================================================================================================
    // Ingredient class
    //==============================================================================================================
public static class Ingredient {
    public String name;
    public String info;
    public String imagePath;
    public List<String> nutrients;

    public int calories;
    public double protein;
    public double carbs;
    public double fats;
    public double fiber;
    public double sugars;

    public Ingredient(String name, String info, String imagePath) {
        this.name = name;
        this.info = info;
        this.imagePath = imagePath;
        this.nutrients = new ArrayList<>();
    }
}

    //==============================================================================================================
    // Recipe class
    //==============================================================================================================
public static class Recipe {
    public int id;
    public String name;                // recipe_name
    public int numIngredients;         // num_ingredients
    public double ingredientCostSum;   // ingredient_cost_sum
    public double costCook;            // cost_cook
    public double costPerServing;      // cost_per_serving
    public double cartCost;            // cart_cost
    public String nutritionGrade;      // nutrition_grade
    public String instructions;        // instructions
    public String imagePath;

    public Recipe(int id, String name, int numIngredients, double ingredientCostSum,
                  double costCook, double costPerServing, double cartCost,
                  String nutritionGrade, String instructions, String imagePath) {
        this.id = id;
        this.name = name;
        this.numIngredients = numIngredients;
        this.ingredientCostSum = ingredientCostSum;
        this.costCook = costCook;
        this.costPerServing = costPerServing;
        this.cartCost = cartCost;
        this.nutritionGrade = nutritionGrade;
        this.instructions = instructions;
        this.imagePath = imagePath;
    }
}

    //==============================================================================================================
    // Instance fields
    //==============================================================================================================
    private final Set<String> favoriteIngredientNames = new HashSet<>();
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();
    private List<Ingredient> ingredients = new ArrayList<>();
    private final List<Recipe> recipes = new ArrayList<>();
    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private JPanel sidebarPanel;
    private JSplitPane splitPane;
    private boolean sidebarVisible = false;
    private Timer sidebarTimer;
    private final int sidebarWidth = 480;
    private boolean showingFavorites = false;
    private boolean showingIngredients = true;
    private final JButton allButton = new JButton("All");
    private final JButton favoritesButton = new JButton("Favorites");
    private final JTextField searchField = new JTextField(20);
    private final JTabbedPane mainTabs = new JTabbedPane();
    private final Path favoritesDir = Paths.get("src/pages/favorites");

    // Database connection info
        private final String dbUrl = String.format(
        "jdbc:mysql://%s:3306/%s",
        System.getenv("host"),
        System.getenv("database")
);

private final String dbUser = System.getenv("user");
private final String dbPassword = System.getenv("password");



    //==============================================================================================================
    // Constructors
    //==============================================================================================================
    public Page2() { this(null); }

    public Page2(IngredientSelectionListener listener) {
        // --- DEBUG: Check environment variables ---
System.out.println("ENV host=" + System.getenv("host"));
System.out.println("ENV database=" + System.getenv("database"));
System.out.println("ENV user=" + System.getenv("user"));
System.out.println("ENV password=" + System.getenv("password"));

// --- DEBUG: Test DB connection ---
try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
    System.out.println("✅ DB CONNECTION OK");
} catch (SQLException e) {
    System.out.println("❌ DB CONNECTION FAILED");
    System.out.println("URL = " + dbUrl);
    System.out.println("USER = " + dbUser);
    System.out.println("PASS = " + dbPassword);
    e.printStackTrace();     // <-- VERY IMPORTANT
}

        this.selectionListener = listener;

        setLayout(new BorderLayout());
        try { Files.createDirectories(favoritesDir); } catch (IOException e) { e.printStackTrace(); }

        loadIngredientsFromDB();
        loadRecipesFromDB();
        loadSavedFavorites();

        buildUI();

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(getWidth());
            sidebarVisible = false;
        });
    }

    // Add this constructor alongside your existing ones
public Page2(IngredientSelectionListener listener, boolean sidebarVisible) {
    this.selectionListener = listener;
    this.sidebarVisible = sidebarVisible;   // ensure sidebarVisible is defined as a field

    setLayout(new BorderLayout());
    try { Files.createDirectories(favoritesDir); } catch (IOException e) { e.printStackTrace(); }

    loadIngredientsFromDB();
    loadRecipesFromDB();
    loadSavedFavorites();

    buildUI();

    SwingUtilities.invokeLater(() -> {
        if (splitPane != null) splitPane.setDividerLocation(getWidth());
    });
}

    //==============================================================================================================
    // Database loading
    //==============================================================================================================
public void loadIngredientsFromDB() {
    ingredients.clear();
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT ingredient_ID, ingredient_name, calories, protein, carbs, fats, fiber, sugars " +
             "FROM Ingredients"
         )) {

        while (rs.next()) {
        Ingredient ing = new Ingredient(
            rs.getString("ingredient_name"),
            "", // info
            ""  // imagePath
        );


            
            ing.calories = rs.getInt("calories");
            ing.protein = rs.getDouble("protein");
            ing.carbs = rs.getDouble("carbs");
            ing.fats = rs.getDouble("fats");
            ing.fiber = rs.getDouble("fiber");
            ing.sugars = rs.getDouble("sugars");


            ingredients.add(ing);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void loadRecipesFromDB() {
    recipes.clear();

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT recipe_ID, recipe_name, num_ingredients, ingredient_cost_sum, cost_cook, " +
             "cost_per_serving, cart_cost, nutrition_grade, instructions " +
             "FROM Recipes"
         )) {

        while (rs.next()) {
            recipes.add(new Recipe(
                rs.getInt("recipe_ID"),
                rs.getString("recipe_name"),
                rs.getInt("num_ingredients"),
                rs.getDouble("ingredient_cost_sum"),
                rs.getDouble("cost_cook"),
                rs.getDouble("cost_per_serving"),
                rs.getDouble("cart_cost"),
                rs.getString("nutrition_grade") != null ? rs.getString("nutrition_grade") : "Unknown",
                rs.getString("instructions") != null ? rs.getString("instructions") : "No instructions available.",
                "" // imagePath (optional)
            ));
        }

        System.out.println("Loaded recipes: " + recipes.size());

    } catch (SQLException e) {
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
        allButton.addActionListener(e -> showAll());
        favoritesButton.addActionListener(e -> showFavorites());

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(allButton);
        topPanel.add(favoritesButton);
        add(topPanel, BorderLayout.NORTH);

        mainTabs.addTab("Ingredients", null);
        mainTabs.addTab("Recipes", null);
        mainTabs.addChangeListener(e -> {
            showingIngredients = mainTabs.getSelectedIndex() == 0;
            showAll();
        });
        add(mainTabs, BorderLayout.SOUTH);

        gridPanel = new JPanel();
        gridPanel.setBackground(Color.WHITE);
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
    // Grid Panels
    //==============================================================================================================
    private JPanel buildGridPanelForIngredients(List<Ingredient> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Ingredient ing : items) if (matchesSearch(ing.name)) grid.add(createIngredientCard(ing));
        return grid;
    }

private JPanel buildGridPanelForRecipes(List<Recipe> items) {
    JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));  // Use a flow layout to wrap items
    grid.setBackground(Color.WHITE);
    
    // Check if there are any recipes
    if (items != null && !items.isEmpty()) {
        for (Recipe r : items) {
            // Ensure the recipe name is valid and add a card for each recipe
            if (matchesSearch(r.name)) {
                grid.add(createRecipeCard(r));  // create the card for the recipe
            }
        }
    }

    return grid;
}



    //==============================================================================================================
    // Cards
    //==============================================================================================================
    private JPanel createIngredientCard(Ingredient ing) {
        JPanel card = baseCard(ing.name, ing.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(ing); }
        });
        return card;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = baseCard(r.name, r.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(r); }
        });
        return card;
    }

    private JPanel baseCard(String nameText, String imageUrl) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
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
            image.setText("[No Image]");
            image.setForeground(Color.GRAY);
        }

        card.add(image);
        card.add(Box.createVerticalStrut(8));
        card.add(name);
        return card;
    }

    //==============================================================================================================
    // Sidebar
    //==============================================================================================================
    private JPanel wrapSidebarWithCloseButton(JPanel contentPanel) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(contentPanel, BorderLayout.CENTER);

    JButton closeButton = new JButton("X");
    closeButton.setMargin(new Insets(2, 5, 2, 5)); // smaller button
    closeButton.addActionListener(e -> animateSidebar(false));
    
    JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    topRight.setOpaque(false);
    topRight.add(closeButton);
    
    wrapper.add(topRight, BorderLayout.NORTH);

    return wrapper;
}

    private void showSidebar(Ingredient ing) {
        sidebarPanel.removeAll();
        sidebarPanel.add(wrapSidebarWithCloseButton(createSidebarContent(r)), BorderLayout.CENTER);
        animateSidebar(true);
    }

    private void showSidebar(Recipe r) {
        sidebarPanel.removeAll();
        sidebarPanel.add(wrapSidebarWithCloseButton(createSidebarContent(r)), BorderLayout.CENTER);
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(ing.name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(nameLabel);

        if (ing.nutrients != null && !ing.nutrients.isEmpty()) {
            JTextArea nutrientArea = new JTextArea(String.join("\n", ing.nutrients));
            nutrientArea.setEditable(false);
            panel.add(new JScrollPane(nutrientArea));
        }

        return panel;
    }

    private JPanel createSidebarContent(Recipe r) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    JLabel nameLabel = new JLabel(r.name, SwingConstants.CENTER);
    nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    panel.add(nameLabel);

    // Nutrition Grade
if (r.nutritionGrade != null && !r.nutritionGrade.isEmpty()) {
    JLabel nutritionLabel = new JLabel(r.nutritionGrade, SwingConstants.CENTER);
    nutritionLabel.setOpaque(true);
    nutritionLabel.setForeground(Color.WHITE);
    nutritionLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
    nutritionLabel.setHorizontalAlignment(SwingConstants.CENTER);
    nutritionLabel.setPreferredSize(new Dimension(60, 30));

    switch (r.nutritionGrade.toUpperCase()) {
        case "A": nutritionLabel.setBackground(new Color(0, 153, 0)); break;       // Green
        case "B": nutritionLabel.setBackground(new Color(153, 204, 0)); break;     // Light Green
        case "C": nutritionLabel.setBackground(new Color(255, 204, 0)); break;     // Yellow
        case "D": nutritionLabel.setBackground(new Color(255, 153, 51)); break;    // Orange
        case "E": nutritionLabel.setBackground(new Color(204, 0, 0)); break;       // Red
        default: nutritionLabel.setBackground(Color.GRAY);                          // Unknown
    }

    nutritionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(Box.createVerticalStrut(10));
    panel.add(nutritionLabel);
}


    // Instructions
    if (r.instructions != null && !r.instructions.isEmpty()) {
        JTextArea instructionsArea = new JTextArea(r.instructions);
        instructionsArea.setEditable(false);
        panel.add(new JScrollPane(instructionsArea));
    }

    // Cost breakdown
    JPanel costPanel = new JPanel();
    costPanel.setLayout(new GridLayout(3, 2));
    costPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    costPanel.add(new JLabel("Num Ingredients:"));
    costPanel.add(new JLabel(String.valueOf(r.numIngredients)));
    costPanel.add(new JLabel("Total Ingredient Cost:"));
    costPanel.add(new JLabel(String.format("$%.2f", r.ingredientCostSum)));
    costPanel.add(new JLabel("Cost per Serving:"));
    costPanel.add(new JLabel(String.format("$%.2f", r.costPerServing)));
    panel.add(costPanel);

    return panel;
}

    //==============================================================================================================
    // Favorites / Filtering
    //==============================================================================================================
    private void showFavorites() { showingFavorites = true; refreshGrid(); }
    private void showAll() { showingFavorites = false; refreshGrid(); }

    private void refreshGrid() {
    JPanel newGrid;

    if (showingIngredients) {
        List<Ingredient> ingredientList = ingredients;
        newGrid = buildGridPanelForIngredients(ingredientList);
    } else {
        // Temporarily ignore favorites to make sure recipes show
        List<Recipe> recipeList = recipes;

        if (recipeList.isEmpty()) {
            newGrid = new JPanel(new BorderLayout());
            JLabel noRecipesLabel = new JLabel("No recipes available", SwingConstants.CENTER);
            noRecipesLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            newGrid.add(noRecipesLabel, BorderLayout.CENTER);
        } else {
            newGrid = buildGridPanelForRecipes(recipeList);
        }
    }

    gridPanel = newGrid;
    scrollPane.setViewportView(gridPanel);

    revalidate();
    repaint();

    // Debug
    System.out.println("refreshGrid called | showingIngredients=" + showingIngredients + " | recipes=" + recipes.size());
}

    private boolean matchesSearch(String name) {
        String q = searchField.getText().trim().toLowerCase();
        return q.isEmpty() || name.toLowerCase().contains(q);
    }
    
    

    //==============================================================================================================
    // Favorite file handling
    //==============================================================================================================
    private void loadSavedFavorites() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(favoritesDir, "*.json")) {
            for (Path path : stream) {
                String content = Files.readString(path);
                JSONObject obj = new JSONObject(content);
                String name = obj.optString("name");
                favoriteIngredientNames.add(name);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public Set<String> getFavoriteIngredientNames() { return favoriteIngredientNames; }
    public Set<Integer> getFavoriteRecipeIds() { return favoriteRecipeIds; }



    public void addIngredientToDB(Ingredient ing) {
    String sql = "INSERT INTO ingredients(name, info, image_path, nutrients) " +
                 "VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE info=?, image_path=?, nutrients=?";
    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, ing.name);
        ps.setString(2, ing.info);
        ps.setString(3, ing.imagePath);
        ps.setString(4, String.join(",", ing.nutrients));

        ps.setString(5, ing.info);
        ps.setString(6, ing.imagePath);
        ps.setString(7, String.join(",", ing.nutrients));

        ps.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }

    ingredients.add(ing);
    refreshGrid();
}



}

