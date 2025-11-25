package pages;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class Page4 extends JPanel {

    //==============================================================================================================
    // Recipe class
    //==============================================================================================================
    public static class Recipe {
        public int recipe_ID;
        public String recipe_name;
        public List<IngredientEntry> recipe_ingredients;
        public double recipe_cos_sum;
        public double cost_cook;
        public double cost_per_serving;
        public double cart_cost;
        public String nutrition_grade;
        public List<String> instructions;
        public String imagePath;
        public String description;

        // New (optional) fields
        public int total_portions = 1; // default 1 if not provided
        public int edible_days = 3;    // default 3 days if not provided

        /**
         * Backwards-compatible constructor (keeps older signature).
         */
        public Recipe(int recipe_ID, String recipe_name, double recipe_cos_sum, double cost_cook,
                      double cost_per_serving, double cart_cost, String nutrition_grade,
                      List<IngredientEntry> recipe_ingredients, List<String> instructions, String description, String imagePath) {
            this.recipe_ID = recipe_ID;
            this.recipe_name = recipe_name;
            this.recipe_cos_sum = recipe_cos_sum;
            this.cost_cook = cost_cook;
            this.cost_per_serving = cost_per_serving;
            this.cart_cost = cart_cost;
            this.nutrition_grade = nutrition_grade;
            this.recipe_ingredients = recipe_ingredients != null ? recipe_ingredients : new ArrayList<>();
            this.instructions = instructions != null ? instructions : new ArrayList<>();
            this.description = description;
            this.imagePath = imagePath;
            // total_portions and edible_days remain defaults
        }


        public Recipe(int recipe_ID, String recipe_name, double recipe_cos_sum, double cost_cook,
                      double cost_per_serving, double cart_cost, String nutrition_grade,
                      List<IngredientEntry> recipe_ingredients, List<String> instructions, String description, String imagePath,
                      int total_portions, int edible_days) {
            this(recipe_ID, recipe_name, recipe_cos_sum, cost_cook, cost_per_serving, cart_cost,
                    nutrition_grade, recipe_ingredients, instructions, description, imagePath);
            this.total_portions = Math.max(1, total_portions);
            this.edible_days = Math.max(0, edible_days);
        }
    }

    //==============================================================================================================
    // IngredientEntry class
    //==============================================================================================================
    public static class IngredientEntry {
        public String name;
        public double quantity;
        public String unit;
        public String info;
        public String imagePath;

        public IngredientEntry(String name, double quantity, String unit, String info, String imagePath) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.info = info;
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
        addButton.addActionListener(e -> openAddRecipeDialog(null));

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
            if (matchesSearch(r.recipe_name))
                grid.add(createRecipeCard(r));
        }

        return grid;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = baseCard(r.recipe_name, r.imagePath);
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
    // Sidebar / Edit Recipe
    //==============================================================================================================
    private void showSidebar(Recipe r) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), r.recipe_name, true);
        dialog.setSize(600, 800);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(r.recipe_name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = loadImageIcon(r.imagePath, 300, 180);
        if (icon != null) imageLabel.setIcon(icon);
        else imageLabel.setText("[No Image]");

        JLabel infoLabel = new JLabel(String.format("<html>Cost Cook: $%.2f<br>Cost per Serving: $%.2f<br>Cart Cost: $%.2f<br>Nutrition: %s<br>Total portions: %d | Good for: %d days</html>",
                r.cost_cook, r.cost_per_serving, r.cart_cost, r.nutrition_grade, r.total_portions, r.edible_days));

        JTextArea descArea = new JTextArea(r.description != null ? r.description : "No description available.");
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(new Color(250, 250, 250));
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 13));

        StringBuilder ingrDisplay = new StringBuilder();
        for (IngredientEntry ie : r.recipe_ingredients) {
            ingrDisplay.append(String.format("%s — %.2f %s\n", ie.name, ie.quantity, ie.unit));
        }
        JTextArea ingrArea = new JTextArea(ingrDisplay.toString());
        ingrArea.setEditable(false);
        ingrArea.setLineWrap(true);
        ingrArea.setWrapStyleWord(true);

        JTextArea instrArea = new JTextArea();
        if (r.instructions != null && !r.instructions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.instructions.size(); i++) sb.append(String.format("%d. %s\n", i + 1, r.instructions.get(i)));
            instrArea.setText(sb.toString());
        } else instrArea.setText("No instructions available.");
        instrArea.setEditable(false);
        instrArea.setLineWrap(true);
        instrArea.setWrapStyleWord(true);

        JButton editButton = new JButton("Edit Recipe");
        editButton.addActionListener(e -> {
            dialog.dispose();
            openAddRecipeDialog(r);
        });

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
        dialog.add(new JLabel("Ingredients:"));
        dialog.add(new JScrollPane(ingrArea));
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(new JLabel("Instructions:"));
        dialog.add(new JScrollPane(instrArea));
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(editButton);
        dialog.add(Box.createVerticalStrut(5));
        dialog.add(deleteButton);

        dialog.setVisible(true);
    }
    //==============================================================================================================
    // Add / Edit Recipe Dialog
    //==============================================================================================================
    private void openAddRecipeDialog(Recipe existing) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                existing != null ? "Edit Recipe" : "Add Recipe", true);
        dialog.setSize(500, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(mainPanel);
        dialog.add(scroll, BorderLayout.CENTER);

        // Recipe name
        JTextField nameField = new JTextField(existing != null ? existing.recipe_name : "", 20);
        mainPanel.add(new JLabel("Recipe Name:"));
        mainPanel.add(nameField);

        // Description
        JTextArea descArea = new JTextArea(existing != null ? existing.description : "", 4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        mainPanel.add(new JLabel("Description:"));
        mainPanel.add(new JScrollPane(descArea));

        // Costs
        JTextField costCookField = new JTextField(existing != null ? String.valueOf(existing.cost_cook) : "0.0", 10);
        JTextField costPerField = new JTextField(existing != null ? String.valueOf(existing.cost_per_serving) : "0.0", 10);
        JTextField cartCostField = new JTextField(existing != null ? String.valueOf(existing.cart_cost) : "0.0", 10);
        JTextField nutritionField = new JTextField(existing != null ? existing.nutrition_grade : "", 5);

        mainPanel.add(new JLabel("Cost Cook:"));
        mainPanel.add(costCookField);
        mainPanel.add(new JLabel("Cost per Serving:"));
        mainPanel.add(costPerField);
        mainPanel.add(new JLabel("Cart Cost:"));
        mainPanel.add(cartCostField);
        mainPanel.add(new JLabel("Nutrition Grade:"));
        mainPanel.add(nutritionField);

        // Image
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField imagePathField = new JTextField(existing != null ? existing.imagePath : "", 20);
        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                imagePathField.setText(f.getAbsolutePath());
            }
        });
        imagePanel.add(imagePathField);
        imagePanel.add(browseBtn);
        mainPanel.add(new JLabel("Image:"));
        mainPanel.add(imagePanel);

        // Ingredients
        mainPanel.add(new JLabel("Ingredients:"));
        JPanel ingredientsPanel = new JPanel();
        ingredientsPanel.setLayout(new BoxLayout(ingredientsPanel, BoxLayout.Y_AXIS));
        mainPanel.add(ingredientsPanel);

        List<IngredientEntry> ingredientList = existing != null
                ? new ArrayList<>(existing.recipe_ingredients)
                : new ArrayList<>();

        // Final wrapper for safe lambda reference
        final class RunnableHolder { Runnable r; }
        RunnableHolder refreshHolder = new RunnableHolder();

        refreshHolder.r = () -> {
            ingredientsPanel.removeAll();
            ingredientsPanel.setBackground(mainPanel.getBackground());

            for (IngredientEntry ie : ingredientList) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.setOpaque(false);

                JTextField nameF = new JTextField(ie.name, 10);
                JTextField qtyF = new JTextField(String.valueOf(ie.quantity), 5);
                JComboBox<String> unitBox = new JComboBox<>(new String[]{"g","kg","oz","lb","ml","l","tsp","tbsp","cup","unit"});
                unitBox.setSelectedItem(ie.unit != null ? ie.unit : "unit");
                JTextField infoF = new JTextField(ie.info != null ? ie.info : "", 10);

                JButton removeBtn = new JButton("Remove");
                removeBtn.addActionListener(ev -> {
                    ingredientList.remove(ie);
                    SwingUtilities.invokeLater(refreshHolder.r); // safe repaint
                });

                row.add(nameF);
                row.add(qtyF);
                row.add(unitBox);
                row.add(infoF);
                row.add(removeBtn);

                ingredientsPanel.add(row);
            }

            ingredientsPanel.revalidate();
            ingredientsPanel.repaint();
            SwingUtilities.invokeLater(() -> scroll.revalidate());
        };

        // Initial display
        refreshHolder.r.run();

        // Add ingredient button
        JButton addIngredientBtn = new JButton("Add Ingredient");
        addIngredientBtn.addActionListener(e -> {
            Page2.IngredientSelectionListener listener = json -> {
                try {
                    String name = json.optString("name");
                    String info = json.optString("info", "");
                    String img = json.optString("imagePath", "");
                    double qty = Double.parseDouble(json.optString("quantity", "0"));
                    String unit = json.optString("unit", "unit");

                    ingredientList.add(new IngredientEntry(name, qty, unit, info, img));
                    SwingUtilities.invokeLater(refreshHolder.r);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            };

            Page2 page2 = new Page2(listener, true);
            JDialog miniDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(dialog), "Select Ingredient", true);
            miniDialog.setSize(600, 600);
            miniDialog.setLocationRelativeTo(dialog);
            miniDialog.setLayout(new BorderLayout());
            miniDialog.add(page2, BorderLayout.CENTER);
            miniDialog.setVisible(true);
        });
        mainPanel.add(addIngredientBtn);

        // Instructions
        JTextArea instrArea = new JTextArea();
        if (existing != null && existing.instructions != null) {
            StringBuilder sb = new StringBuilder();
            for (String s : existing.instructions) sb.append(s).append("\n");
            instrArea.setText(sb.toString());
        }
        instrArea.setLineWrap(true);
        instrArea.setWrapStyleWord(true);
        mainPanel.add(new JLabel("Instructions (one per line):"));
        mainPanel.add(new JScrollPane(instrArea));

        // New fields: total_portions and edible_days
        JTextField totalPortionsField = new JTextField(existing != null ? String.valueOf(existing.total_portions) : "1", 5);
        JTextField edibleDaysField = new JTextField(existing != null ? String.valueOf(existing.edible_days) : "3", 5);
        JPanel extrasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        extrasPanel.add(new JLabel("Total portions:"));
        extrasPanel.add(totalPortionsField);
        extrasPanel.add(Box.createHorizontalStrut(10));
        extrasPanel.add(new JLabel("Edible days:"));
        extrasPanel.add(edibleDaysField);
        mainPanel.add(extrasPanel);

        // Save button
        JButton saveBtn = new JButton("Save Recipe");
        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Recipe must have a name.");
                    return;
                }

                String desc = descArea.getText().trim();
                double costCook = safeParseDouble(costCookField.getText());
                double costPer = safeParseDouble(costPerField.getText());
                double cartCost = safeParseDouble(cartCostField.getText());
                String nutrition = nutritionField.getText().trim();
                String imagePath = imagePathField.getText().trim();

                // Update ingredient entries from UI
                for (int i = 0; i < ingredientsPanel.getComponentCount(); i++) {
                    JPanel row = (JPanel) ingredientsPanel.getComponent(i);
                    JTextField nameF = (JTextField) row.getComponent(0);
                    JTextField qtyF = (JTextField) row.getComponent(1);
                    JComboBox<?> unitBox = (JComboBox<?>) row.getComponent(2);
                    JTextField infoF = (JTextField) row.getComponent(3);

                    IngredientEntry ie = ingredientList.get(i);
                    ie.name = nameF.getText().trim();
                    ie.quantity = safeParseDouble(qtyF.getText());
                    ie.unit = (String) unitBox.getSelectedItem();
                    ie.info = infoF.getText().trim();
                }

                // Instructions
                List<String> instrList = new ArrayList<>();
                for (String line : instrArea.getText().split("\\n")) {
                    if (!line.trim().isEmpty()) instrList.add(line.trim());
                }

                int totalPortions = Math.max(1, safeParseInt(totalPortionsField.getText()));
                int edibleDays = Math.max(0, safeParseInt(edibleDaysField.getText()));

                Recipe r;
                if (existing != null) {
                    existing.recipe_name = name;
                    existing.description = desc;
                    existing.cost_cook = costCook;
                    existing.cost_per_serving = costPer;
                    existing.cart_cost = cartCost;
                    existing.nutrition_grade = nutrition;
                    existing.imagePath = imagePath;
                    existing.recipe_ingredients = ingredientList;
                    existing.instructions = instrList;
                    existing.total_portions = totalPortions;
                    existing.edible_days = edibleDays;
                    r = existing;
                } else {
                    int newID = recipes.stream().mapToInt(rec -> rec.recipe_ID).max().orElse(0) + 1;
                    r = new Recipe(newID, name, 0.0, costCook, costPer, cartCost, nutrition, ingredientList, instrList, desc, imagePath, totalPortions, edibleDays);
                    recipes.add(r);
                }

                saveRecipesToFile();
                refreshGrid();
                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to save recipe: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        mainPanel.add(saveBtn);
        dialog.setVisible(true);
    }


    //==============================================================================================================
    // Load / Save Recipes
    //==============================================================================================================
    List<Recipe> loadRecipes(String filePath) {
        List<Recipe> list = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine(); // header (may be null)
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t");


                if (p.length < 9) continue;

                int id = safeParseInt(p[0]);
                String name = p.length > 1 ? p[1] : "";
                double cos_sum = p.length > 2 ? safeParseDouble(p[2]) : 0.0;
                double costCook = p.length > 3 ? safeParseDouble(p[3]) : 0.0;
                double costPer = p.length > 4 ? safeParseDouble(p[4]) : 0.0;
                double cart = p.length > 5 ? safeParseDouble(p[5]) : 0.0;
                String nutrition = p.length > 6 ? p[6] : "";
                String desc = p.length > 7 ? p[7] : "";
                String img = p.length > 8 ? p[8] : "";

                // Defaults for new fields
                int totalPortions = 1;
                int edibleDays = 3;
                int ingredientsIndex = 9;
                int instructionsIndex = 10;

                if (p.length >= 11) {

                    boolean parsedExtras = false;
                    try {
                        totalPortions = safeParseInt(p[9]);
                        edibleDays = safeParseInt(p[10]);
                        ingredientsIndex = 11;
                        instructionsIndex = 12;
                        parsedExtras = true;
                    } catch (Exception ignored) {
                        totalPortions = 1;
                        edibleDays = 3;
                        ingredientsIndex = 9;
                        instructionsIndex = 10;
                    }


                    if (parsedExtras && p.length <= 11) {
                        ingredientsIndex = 11;
                        instructionsIndex = 12;
                    }
                } else {
                    ingredientsIndex = 9;
                    instructionsIndex = 10;
                }

                List<IngredientEntry> ingrList = new ArrayList<>();
                List<String> instrList = new ArrayList<>();

                try {
                    if (p.length > ingredientsIndex && p[ingredientsIndex] != null && !p[ingredientsIndex].isEmpty()) {
                        String ingrJson = new String(Base64.getDecoder().decode(p[ingredientsIndex]));
                        JSONArray arr = new JSONArray(ingrJson);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            IngredientEntry ie = new IngredientEntry(
                                    o.optString("name"),
                                    o.optDouble("quantity", 0.0),
                                    o.optString("unit"),
                                    o.optString("info"),
                                    o.optString("imagePath")
                            );
                            ingrList.add(ie);
                        }
                    }
                } catch (Exception ex) {
                }

                try {
                    if (p.length > instructionsIndex && p[instructionsIndex] != null && !p[instructionsIndex].isEmpty()) {
                        String instrJson = new String(Base64.getDecoder().decode(p[instructionsIndex]));
                        JSONArray ia = new JSONArray(instrJson);
                        for (int i = 0; i < ia.length(); i++) instrList.add(ia.getString(i));
                    }
                } catch (Exception ex) {
                }

                Recipe r = new Recipe(id, name, cos_sum, costCook, costPer, cart, nutrition, ingrList, instrList, desc, img, totalPortions, edibleDays);
                list.add(r);
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
                fw.write("recipe_ID\trecipe_name\trecipe_cos_sum\tcost_cook\tcost_per_serving\tcart_cost\tnutrition_grade\tdescription\timagePath\ttotal_portions\tedible_days\tingredients_base64\tinstructions_base64\n");
                for (Recipe r : recipes) {
                    JSONArray ingrArr = new JSONArray();
                    for (IngredientEntry ie : r.recipe_ingredients) {
                        JSONObject o = new JSONObject();
                        o.put("name", ie.name);
                        o.put("quantity", ie.quantity);
                        o.put("unit", ie.unit);
                        o.put("info", ie.info != null ? ie.info : "");
                        o.put("imagePath", ie.imagePath != null ? ie.imagePath : "");
                        ingrArr.put(o);
                    }
                    String ingrBase64 = Base64.getEncoder().encodeToString(ingrArr.toString().getBytes());

                    JSONArray instrArr = new JSONArray();
                    for (String s : r.instructions) instrArr.put(s);
                    String instrBase64 = Base64.getEncoder().encodeToString(instrArr.toString().getBytes());

                    fw.write(String.format("%d\t%s\t%.2f\t%.2f\t%.2f\t%.2f\t%s\t%s\t%s\t%d\t%d\t%s\t%s\n",
                            r.recipe_ID,
                            r.recipe_name.replace("\t", " ").replace("\n", " "),
                            r.recipe_cos_sum,
                            r.cost_cook,
                            r.cost_per_serving,
                            r.cart_cost,
                            r.nutrition_grade != null ? r.nutrition_grade : "",
                            r.description != null ? r.description.replace("\n", " ") : "",
                            r.imagePath != null ? r.imagePath : "",
                            r.total_portions,
                            r.edible_days,
                            ingrBase64,
                            instrBase64
                    ));
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
                ? recipes.stream().filter(r -> favoriteRecipeIds.contains(r.recipe_ID)).toList()
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