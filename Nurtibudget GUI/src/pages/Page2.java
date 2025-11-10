package pages;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import py4j.ClientServer;
import py4j.Py4JNetworkException;

import bridge.IKrogerWrapper;

//======================================================================================================================
// Page2 - Ingredients & Recipes with Py4J integration (Kroger API)
//======================================================================================================================
public class Page2 extends JPanel {

    //==============================================================================================================
    // Ingredient class
    //==============================================================================================================
    public static class Ingredient {
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
        public List<String> nutrients = new ArrayList<>();

        public Ingredient(int id, String name, double cost, int calories,
                          double protein, double carbs, double fat,
                          String description, String imagePath) {
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

        // Constructor for Python results
        public Ingredient(String name, String info, String imagePath) {
            this.id = -1;
            this.name = name;
            this.description = info;
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
        public String nutriGrade;

        public Recipe(int id, String name, double cost, int calories,
                      int protein, int carbs, double fat,
                      String description, String imagePath) {
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

    private List<Ingredient> ingredients = new ArrayList<>();
    private List<Recipe> recipes = new ArrayList<>();

    private boolean filterVegetarian = false;
    private boolean filterVegan = false;
    private boolean filterGlutenFree = false;

    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private JPanel sidebarPanel;
    private JSplitPane splitPane;
    private Timer sidebarTimer;

    private boolean showingFavorites = false;
    private boolean showingIngredients = true;

    private final int sidebarWidth = 480;
    private final JComboBox<String> filterDropdown =
            new JComboBox<>(new String[]{"All", "A", "B", "C", "D", "E"});

    private final JButton allButton = new JButton("All");
    private final JButton favoritesButton = new JButton("Favorites");
    private final JTextField searchField = new JTextField(20);
    private final JTabbedPane mainTabs = new JTabbedPane();

    private ClientServer clientServer;
    private IKrogerWrapper pythonEntry;

    private final Path favoritesDir = Paths.get("src/pages/favorites");

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public Page2() {
        setLayout(new BorderLayout());
        try {
            Files.createDirectories(favoritesDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initPythonConnection();

        ingredients = loadIngredientsFromPython("");
        recipes = loadRecipes("/pages/text/recipes.txt");

        buildUI();

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(getWidth());
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
        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshGrid(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshGrid(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshGrid(); }
        });

        allButton.addActionListener(e -> showAll());
        favoritesButton.addActionListener(e -> showFavorites());

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(allButton);
        topPanel.add(favoritesButton);

        topPanel.add(new JLabel("NutriScore:"));
        filterDropdown.setPreferredSize(new Dimension(120, 28));
        filterDropdown.addActionListener(e -> refreshGrid());
        topPanel.add(filterDropdown);

        // Dietary filters
        JButton dietFilterButton = new JButton("Dietary ▼");
        JPopupMenu dietMenu = new JPopupMenu();
        JCheckBoxMenuItem vegetarianItem = new JCheckBoxMenuItem("Vegetarian");
        JCheckBoxMenuItem veganItem = new JCheckBoxMenuItem("Vegan");
        JCheckBoxMenuItem glutenFreeItem = new JCheckBoxMenuItem("Gluten-Free");

        vegetarianItem.addActionListener(e -> { filterVegetarian = vegetarianItem.isSelected(); refreshGrid(); });
        veganItem.addActionListener(e -> { filterVegan = veganItem.isSelected(); refreshGrid(); });
        glutenFreeItem.addActionListener(e -> { filterGlutenFree = glutenFreeItem.isSelected(); refreshGrid(); });

        dietMenu.add(vegetarianItem);
        dietMenu.add(veganItem);
        dietMenu.add(glutenFreeItem);

        dietFilterButton.addActionListener(e ->
                dietMenu.show(dietFilterButton, 0, dietFilterButton.getHeight()));
        topPanel.add(dietFilterButton);

        add(topPanel, BorderLayout.NORTH);

        mainTabs.addTab("Ingredients", null);
        mainTabs.addTab("Recipes", null);
        mainTabs.addChangeListener(e -> {
            showingIngredients = mainTabs.getSelectedIndex() == 0;
            showAll();
        });

        add(mainTabs, BorderLayout.SOUTH);

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
    // Grid Builders
    //==============================================================================================================
    private JPanel buildGridPanelForIngredients(List<Ingredient> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Ingredient ing : items)
            grid.add(createIngredientCard(ing));
        return grid;
    }

    private JPanel buildGridPanelForRecipes(List<Recipe> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Recipe r : items)
            grid.add(createRecipeCard(r));
        return grid;
    }

    //==============================================================================================================
    // Cards
    //==============================================================================================================
    private JPanel createIngredientCard(Ingredient ing) {
        JPanel card = baseCard(ing.name, ing.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(ing); }
        });
        return card;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = baseCard(r.name, r.imagePath);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) { showSidebar(r); }
        });
        return card;
    }

    private JPanel baseCard(String nameText, String imagePath) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        card.setBackground(new Color(250, 250, 250));
        card.setPreferredSize(new Dimension(180, 180));

        JLabel name = new JLabel(nameText, SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel image = new JLabel();
        image.setAlignmentX(Component.CENTER_ALIGNMENT);
        image.setPreferredSize(new Dimension(120, 90));

        ImageIcon icon = loadImageIcon("images/" + imagePath, 120, 90);
        if (icon != null) image.setIcon(icon);
        else image.setText("[No Image]");

        card.add(image);
        card.add(Box.createVerticalStrut(8));
        card.add(name);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(new Color(230, 240, 255)); }
            public void mouseExited(java.awt.event.MouseEvent e) { card.setBackground(new Color(250, 250, 250)); }
        });

        return card;
    }

    //==============================================================================================================
    // Sidebar
    //==============================================================================================================
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

    private JPanel createSidebarContent(Ingredient ing) {
        return sidebarTemplate(
                ing.name, ing.imagePath,
                String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %.1f<br>Carbs: %.1f<br>Fat: %.1f</html>",
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
        exitButton.addActionListener(e -> animateSidebar(false));
        exitButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel infoLabel = new JLabel(info);
        JTextArea descArea = new JTextArea(desc == null ? "" : desc);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);

        JButton favoriteButton = new JButton(
                isIngredient
                        ? (favoriteIngredientIds.contains(id) ? "★ Favorite" : "☆ Favorite")
                        : (favoriteRecipeIds.contains(id) ? "★ Favorite" : "☆ Favorite")
        );
        favoriteButton.addActionListener(e -> {
            if (isIngredient) toggleFavoriteIngredient(id, favoriteButton);
            else toggleFavoriteRecipe(id, favoriteButton);
        });

        panel.add(exitButton);
        panel.add(nameLabel);
        panel.add(infoLabel);
        panel.add(new JScrollPane(descArea));
        panel.add(favoriteButton);

        return panel;
    }

    private void animateSidebar(boolean show) {
        if (sidebarTimer != null && sidebarTimer.isRunning()) sidebarTimer.stop();

        final int[] current = {splitPane.getDividerLocation()};
        int targetDividerLocation = show ? getWidth() - sidebarWidth : getWidth();

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
    }

    //==============================================================================================================
    // Favorites / Filtering
    //==============================================================================================================
    private void toggleFavoriteIngredient(int id, JButton button) {
        if (favoriteIngredientIds.remove(id)) button.setText("☆ Favorite");
        else { favoriteIngredientIds.add(id); button.setText("★ Favorite"); }
        refreshGrid();
    }

    private void toggleFavoriteRecipe(int id, JButton button) {
        if (favoriteRecipeIds.remove(id)) button.setText("☆ Favorite");
        else { favoriteRecipeIds.add(id); button.setText("★ Favorite"); }
        refreshGrid();
    }

    private void showFavorites() { showingFavorites = true; refreshGrid(); }
    private void showAll() { showingFavorites = false; refreshGrid(); }

    //==============================================================================================================
    // Grid refresh
    //==============================================================================================================
    private void refreshGrid() {
        String query = searchField.getText().trim().toLowerCase();
        List<?> list;

        if (showingIngredients) {
            list = showingFavorites
                    ? ingredients.stream().filter(i -> favoriteIngredientIds.contains(i.id)).toList()
                    : ingredients.stream().filter(i -> query.isEmpty() || i.name.toLowerCase().contains(query)).toList();
            gridPanel = buildGridPanelForIngredients((List<Ingredient>) list);
        } else {
            list = showingFavorites
                    ? recipes.stream().filter(r -> favoriteRecipeIds.contains(r.id)).toList()
                    : recipes.stream().filter(r -> query.isEmpty() || r.name.toLowerCase().contains(query)).toList();
            gridPanel = buildGridPanelForRecipes((List<Recipe>) list);
        }

        scrollPane.setViewportView(gridPanel);
        revalidate();
        repaint();
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
                        obj.optString("info", ""),
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
            String line;
            br.readLine(); // skip header
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

    private ImageIcon loadImageIcon(String path, int width, int height) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) return null;
            ImageIcon icon = new ImageIcon(resource);
            Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
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
