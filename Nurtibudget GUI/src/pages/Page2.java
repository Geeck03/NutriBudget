package pages;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import py4j.ClientServer;
import py4j.Py4JNetworkException;

//======================================================================================================================
// Page2 - Ingredients & Recipes with Py4J integration (for py4j0.10.9.9.jar)
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
        public int protein;
        public int carbs;
        public double fat;
        public String description;
        public String imagePath;

        public Ingredient(int id, String name, double cost, int calories, int protein, int carbs,
                          double fat, String description, String imagePath) {
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

    private final List<Ingredient> ingredients;
    private final List<Recipe> recipes;

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

    private ClientServer pyGateway;

    //==============================================================================================================
    // Constructor
    //==============================================================================================================
    public Page2() {
        setLayout(new BorderLayout());

        // Connect to Python Py4J server
        try {
            pyGateway = new ClientServer(null);
            System.out.println("✅ Connected to Python Py4J server on port 25333");
        } catch (Py4JNetworkException e) {
            System.err.println("❌ Could not connect to Python server: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize Py4J ClientServer.");
            e.printStackTrace();
        }

        ingredients = loadIngredientsFromPython(""); // initial load
        recipes = loadRecipes("text/recipes.txt");

        buildUI();

        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(getWidth());
            sidebarVisible = false;
        });
    }

    //==============================================================================================================
    // UI
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
    // Grid
    //==============================================================================================================
    private JPanel buildGridPanelForIngredients(List<Ingredient> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);
        for (Ingredient ing : items)
            if (matchesSearch(ing.name)) grid.add(createIngredientCard(ing));
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

    private JPanel baseCard(String nameText, String imagePath) {
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

        ImageIcon icon = loadImageIcon("images/" + imagePath, 120, 90);
        if (icon != null) image.setIcon(icon);
        else { image.setText("[No Image]"); image.setForeground(Color.GRAY); }

        card.add(image);
        card.add(Box.createVerticalStrut(8));
        card.add(name);
        return card;
    }

    //==============================================================================================================
    // Sidebar + Animation
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
        return sidebarTemplate(ing.name, ing.imagePath,
                String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %d<br>Carbs: %d<br>Fat: %.1f</html>",
                        ing.cost, ing.calories, ing.protein, ing.carbs, ing.fat),
                ing.description, ing.id, true);
    }
    private JPanel createSidebarContent(Recipe r) {
        return sidebarTemplate(r.name, r.imagePath,
                String.format("<html>Cost: $%.2f<br>Calories: %d<br>Protein: %d<br>Carbs: %d<br>Fat: %.1f</html>",
                        r.cost, r.calories, r.protein, r.carbs, r.fat),
                r.description, r.id, false);
    }

    private JPanel sidebarTemplate(String name, String img, String info, String desc, int id, boolean ingredient) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton close = new JButton("✖");
        close.addActionListener(e -> animateSidebar(false));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        ImageIcon icon = loadImageIcon("images/" + img, sidebarWidth - 80, 250);
        JLabel imageLabel = new JLabel(icon);

        JLabel infoLabel = new JLabel(info);
        JTextArea descArea = new JTextArea(desc == null ? "" : desc);
        descArea.setWrapStyleWord(true); descArea.setLineWrap(true);
        descArea.setEditable(false);

        JButton fav = new JButton(ingredient ?
                (favoriteIngredientIds.contains(id) ? "★ Favorite" : "☆ Favorite")
                : (favoriteRecipeIds.contains(id) ? "★ Favorite" : "☆ Favorite"));
        fav.addActionListener(e -> {
            if (ingredient) toggleFavoriteIngredient(id, fav);
            else toggleFavoriteRecipe(id, fav);
        });

        panel.add(close);
        panel.add(nameLabel);
        panel.add(imageLabel);
        panel.add(infoLabel);
        panel.add(new JScrollPane(descArea));
        panel.add(fav);
        return panel;
    }

    //==============================================================================================================
    // Favorites / Filtering
    //==============================================================================================================
    private void toggleFavoriteIngredient(int id, JButton b) {
        if (favoriteIngredientIds.remove(id)) b.setText("☆ Favorite");
        else { favoriteIngredientIds.add(id); b.setText("★ Favorite"); }
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
        if (showingIngredients) {
            List<Ingredient> list = showingFavorites ?
                    ingredients.stream().filter(i -> favoriteIngredientIds.contains(i.id)).toList() : ingredients;
            gridPanel = buildGridPanelForIngredients(list);
        } else {
            List<Recipe> list = showingFavorites ?
                    recipes.stream().filter(r -> favoriteRecipeIds.contains(r.id)).toList() : recipes;
            gridPanel = buildGridPanelForRecipes(list);
        }
        scrollPane.setViewportView(gridPanel);
        revalidate(); repaint();
    }

    private boolean matchesSearch(String name) {
        String q = searchField.getText().trim().toLowerCase();
        return q.isEmpty() || name.toLowerCase().contains(q);
    }

    //==============================================================================================================
    // Data loading (Python + file)
    //==============================================================================================================
    private List<Ingredient> loadIngredientsFromPython(String query) {
        List<Ingredient> list = new ArrayList<>();
        if (pyGateway == null) return list;
        try {
            Object wrapper = pyGateway.getPythonServerEntryPoint(new Class[]{});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) wrapper.getClass().getMethod("search", String.class, int.class)
                            .invoke(wrapper, query, 20);
            for (Map<String, Object> r : results) {
                list.add(new Ingredient(
                        Integer.parseInt(r.get("id").toString()),
                        r.get("name").toString(),
                        Double.parseDouble(r.get("price").toString()),
                        Integer.parseInt(r.get("calories").toString()),
                        Integer.parseInt(r.get("protein").toString()),
                        Integer.parseInt(r.get("carbs").toString()),
                        Double.parseDouble(r.get("fat").toString()),
                        r.get("description").toString(),
                        ""
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
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

    private ImageIcon loadImageIcon(String path, int w, int h) {
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/" + path)));
            Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) { return null; }
    }
}
