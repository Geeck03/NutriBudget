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
    private final Set<String> favoriteIngredientNames = new HashSet<>();
    private final Set<Integer> favoriteRecipeIds = new HashSet<>();

    private List<Ingredient> ingredients;
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

    private ClientServer clientServer;
    private IKrogerWrapper pythonEntry;

    private final Path favoritesDir = Paths.get("src/pages/favorites");

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
    // Grid panels
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
    }
}
