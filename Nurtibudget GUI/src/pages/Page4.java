package pages;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import bridge.IKrogerWrapper;
import bridge.Py4JHelper;
import bridge.PyBridgeInvoker;

public class Page4 extends JPanel {

    // --------------------------------------
    // Models
    // --------------------------------------
    public static class Recipe {
        public int recipe_ID;
        public String recipe_name;
        public List<IngredientEntry> recipe_ingredients = new ArrayList<>();
        public double recipe_cos_sum;
        public double cost_cook;
        public double cost_per_serving;
        public double cart_cost;
        public String nutrition_grade;
        public List<String> instructions = new ArrayList<>();
        public String imagePath;
        public String description;
        public int total_portions = 1;
        public int edible_days = 3;

        public Recipe() {}

        public Recipe(int id, String name) { this.recipe_ID = id; this.recipe_name = name; }

        public Recipe(int id,
                      String name,
                      double recipe_cos_sum,
                      double cost_cook,
                      double cost_per_serving,
                      double cart_cost,
                      String nutrition_grade,
                      List<IngredientEntry> recipe_ingredients,
                      List<String> instructions,
                      String description,
                      String imagePath) {
            this.recipe_ID = id;
            this.recipe_name = name;
            this.recipe_cos_sum = recipe_cos_sum;
            this.cost_cook = cost_cook;
            this.cost_per_serving = cost_per_serving;
            this.cart_cost = cart_cost;
            this.nutrition_grade = nutrition_grade != null ? nutrition_grade : "";
            this.recipe_ingredients = recipe_ingredients != null ? recipe_ingredients : new ArrayList<>();
            this.instructions = instructions != null ? instructions : new ArrayList<>();
            this.description = description != null ? description : "";
            this.imagePath = imagePath != null ? imagePath : "";
        }

        public JSONObject toJson() {
            JSONObject ro = new JSONObject();
            ro.put("id", recipe_ID);
            ro.put("name", recipe_name != null ? recipe_name : "");
            ro.put("recipe_cos_sum", recipe_cos_sum);
            ro.put("cost_cook", cost_cook);
            ro.put("cost_per_serving", cost_per_serving);
            ro.put("cart_cost", cart_cost);
            ro.put("nutrition_grade", nutrition_grade != null ? nutrition_grade : "");
            ro.put("description", description != null ? description : "");
            ro.put("imagePath", imagePath != null ? imagePath : "");
            ro.put("total_portions", total_portions);
            ro.put("edible_days", edible_days);

            JSONArray ings = new JSONArray();
            for (IngredientEntry ie : recipe_ingredients) ings.put(ie.toJson());
            ro.put("ingredients", ings);

            JSONArray instr = new JSONArray();
            for (String s : instructions) instr.put(s);
            ro.put("instructions", instr);
            return ro;
        }

        public static Recipe fromJson(JSONObject o) {
            Recipe r = new Recipe();
            r.recipe_ID = o.optInt("id", 0);
            r.recipe_name = o.optString("name", "");
            r.recipe_cos_sum = o.optDouble("recipe_cos_sum", 0.0);
            r.cost_cook = o.optDouble("cost_cook", 0.0);
            r.cost_per_serving = o.optDouble("cost_per_serving", 0.0);
            r.cart_cost = o.optDouble("cart_cost", 0.0);
            r.nutrition_grade = o.optString("nutrition_grade", "");
            r.description = o.optString("description", "");
            r.imagePath = o.optString("imagePath", "");
            r.total_portions = o.optInt("total_portions", 1);
            r.edible_days = o.optInt("edible_days", 3);

            JSONArray ings = o.optJSONArray("ingredients");
            if (ings != null) {
                for (int i = 0; i < ings.length(); i++) {
                    JSONObject io = ings.getJSONObject(i);
                    IngredientEntry ie = IngredientEntry.fromJson(io);
                    r.recipe_ingredients.add(ie);
                }
            }

            JSONArray instr = o.optJSONArray("instructions");
            if (instr != null) {
                for (int k = 0; k < instr.length(); k++) r.instructions.add(instr.getString(k));
            } else {
                if (r.description != null && !r.description.isEmpty()) r.instructions.add(r.description);
            }
            return r;
        }

        @Override
        public String toString() { return recipe_name != null ? recipe_name : ("Recipe " + recipe_ID); }
    }

    public static class IngredientEntry {
        public String name = "";
        public double quantity = 1.0;
        public String unit = "unit";
        public String info = "";
        public String imagePath = "";

        public String external_id = "";
        public double price_per_serving = 0.0;
        public double calories_per_serving = 0.0;
        public String serving_label = "";
        public JSONObject nutrients_per_serving = null;
        public JSONObject kroger_raw = null;

        public double multiplier = 1.0;

        // Last quantity synced to Python for addIngredientToRecipe calls.
        public double lastSyncedQuantity = Double.NaN;

        public IngredientEntry() {}

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            o.put("name", name != null ? name : "");
            o.put("quantity", quantity);
            o.put("unit", unit != null ? unit : "");
            o.put("info", info != null ? info : "");
            o.put("imagePath", imagePath != null ? imagePath : "");
            o.put("external_id", external_id != null ? external_id : "");
            o.put("price_per_serving", price_per_serving);
            o.put("calories_per_serving", calories_per_serving);
            o.put("serving_label", serving_label != null ? serving_label : "");
            o.put("multiplier", multiplier);
            o.put("nutrients_per_serving", nutrients_per_serving != null ? nutrients_per_serving : JSONObject.NULL);
            o.put("kroger_raw", kroger_raw != null ? kroger_raw : JSONObject.NULL);
            if (!Double.isNaN(lastSyncedQuantity)) o.put("lastSyncedQuantity", lastSyncedQuantity);
            return o;
        }



        public static IngredientEntry fromJson(JSONObject o) {
            IngredientEntry ie = new IngredientEntry();
            ie.name = o.optString("name", "");
            ie.quantity = o.optDouble("quantity", 1.0);
            ie.unit = o.optString("unit", "unit");
            ie.info = o.optString("info", "");
            ie.imagePath = o.optString("imagePath", "");
            ie.external_id = o.optString("external_id", "");
            ie.price_per_serving = o.optDouble("price_per_serving", 0.0);
            ie.calories_per_serving = o.optDouble("calories_per_serving", 0.0);
            ie.serving_label = o.optString("serving_label", "");
            ie.multiplier = o.optDouble("multiplier", 1.0);
            if (o.has("nutrients_per_serving") && !o.isNull("nutrients_per_serving")) ie.nutrients_per_serving = o.optJSONObject("nutrients_per_serving");
            if (o.has("kroger_raw") && !o.isNull("kroger_raw")) ie.kroger_raw = o.optJSONObject("kroger_raw");
            if (o.has("lastSyncedQuantity")) ie.lastSyncedQuantity = o.optDouble("lastSyncedQuantity", Double.NaN);
            return ie;
        }
    }

    // --------------------------------------
    // File / storage
    // --------------------------------------
    private static final String CUSTOM_RECIPE_FILE = "src/pages/text/custom_recipes.txt";

    // --------------------------------------
    // UI fields
    // --------------------------------------
    private final DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
    private final JList<Recipe> recipeJList = new JList<>(recipeListModel);
    private final IngredientTableModel ingredientTableModel = new IngredientTableModel();
    private final JTable ingredientTable = new JTable(ingredientTableModel);
    private final JLabel totalsLabel = new JLabel("Cost $0.00  |  Calories 0.0 kcal");
    private Recipe activeRecipe = null;
    private final List<Recipe> recipes = new ArrayList<>();
    private JSpinner totalPortionsSpinner;
    private JSpinner edibleDaysSpinner;
    private boolean suppressDocumentEvents = false;
    private static final Pattern SERVING_REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(g|kg|mg|ml|l|oz|fl\\s?oz|cup|cups|tbsp|tsp|lb|lbs)\\b", Pattern.CASE_INSENSITIVE);

    public Page4() {
        setLayout(new BorderLayout(8, 8));
        loadRecipes(CUSTOM_RECIPE_FILE);
        buildUI();
    }

    private void autoSaveActiveRecipe() {
        if (suppressDocumentEvents) return;
        if (activeRecipe == null) return;
        try { ingredientTableModel.commitEdits(); } catch (Exception ignored) {}
        activeRecipe.recipe_ingredients = ingredientTableModel.getIngredients();
        activeRecipe.total_portions = ((Number) totalPortionsSpinner.getValue()).intValue();
        activeRecipe.edible_days = ((Number) edibleDaysSpinner.getValue()).intValue();
        recomputeAndStore(activeRecipe);
        saveRecipes(CUSTOM_RECIPE_FILE);
        refreshRecipeListModel();

        // --- NEW: sync ingredient->recipe links to Python ---
        if (activeRecipe.recipe_ID > 0) {
            int recipeId = activeRecipe.recipe_ID;
            for (IngredientEntry ie : activeRecipe.recipe_ingredients) {
                if (ie.external_id == null || ie.external_id.isEmpty()) continue;
                int ingId;
                try { ingId = Integer.parseInt(ie.external_id); }
                catch (Exception ex) { continue; } // skip non-numeric external_id
                double q = ie.quantity;
                double last = ie.lastSyncedQuantity;
                boolean needsSync = Double.isNaN(last) || Double.compare(q, last) != 0;
                if (needsSync) {
                    PyBridgeInvoker.addIngredientToRecipe(recipeId, ingId, q);
                    ie.lastSyncedQuantity = q;
                }
            }
            try { saveRecipes(CUSTOM_RECIPE_FILE); } catch (Exception ignored) {}
        }
    }

    private void markDirty() { autoSaveActiveRecipe(); }
    private void markClean() {}

    private boolean confirmSaveIfDirty() {
        autoSaveActiveRecipe();
        return true;
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newBtn = new JButton("+ New Recipe");
        JButton deleteBtn = new JButton("Delete Selected");
        toolbar.add(newBtn);
        toolbar.add(deleteBtn);
        add(toolbar, BorderLayout.NORTH);

        recipeJList.setCellRenderer(new RecipeListCellRenderer());
        refreshRecipeListModel();
        JScrollPane leftScroll = new JScrollPane(recipeJList);

        JPanel editor = new JPanel(new BorderLayout(6, 6));
        editor.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel meta = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        final JTextField nameField = new JTextField();
        final JTextArea descArea = new JTextArea(6, 30); // used for instructions
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        c.gridx = 0; c.gridy = 0; c.gridwidth = 1;
        meta.add(new JLabel("Recipe name:"), c);
        c.gridx = 1; c.gridy = 0; c.gridwidth = 3;
        meta.add(nameField, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 1;
        meta.add(new JLabel("Total portions:"), c);
        c.gridx = 1;
        totalPortionsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        meta.add(totalPortionsSpinner, c);

        c.gridx = 2;
        meta.add(new JLabel("Days good:"), c);
        c.gridx = 3;
        edibleDaysSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 365, 1));
        meta.add(edibleDaysSpinner, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 1;
        meta.add(new JLabel("Instructions:"), c);
        c.gridx = 1; c.gridy = 2; c.gridwidth = 3;
        meta.add(new JScrollPane(descArea), c);

        editor.add(meta, BorderLayout.NORTH);

        ingredientTable.setFillsViewportHeight(true);
        ingredientTable.setRowHeight(28);
        ingredientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = new JScrollPane(ingredientTable);

        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalsPanel.add(totalsLabel);
        centerPanel.add(totalsPanel, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        editor.add(centerPanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addIngredientBtn = new JButton("Add Ingredient");
        JButton customIngredientBtn = new JButton("Custom Ingredient");
        JButton editAmountsBtn = new JButton("Edit amount");
        JButton viewInfoBtn = new JButton("View info");
        JButton deleteIngredientBtn = new JButton("Delete ingredient");
        leftBottom.add(addIngredientBtn);
        leftBottom.add(customIngredientBtn);
        leftBottom.add(editAmountsBtn);
        leftBottom.add(viewInfoBtn);
        leftBottom.add(deleteIngredientBtn);
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(leftBottom, BorderLayout.WEST);
        bottom.add(rightBottom, BorderLayout.EAST);
        editor.add(bottom, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, editor);
        split.setDividerLocation(260);
        add(split, BorderLayout.CENTER);

        newBtn.addActionListener(e -> {
            if (!confirmSaveIfDirty()) return;
            String baseName = "New recipe";
            String input = null;
            while (true) {
                input = (String) JOptionPane.showInputDialog(this, "Enter recipe name:", "New recipe", JOptionPane.PLAIN_MESSAGE, null, null, baseName);
                if (input == null) return;
                input = input.trim();
                if (input.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Recipe name cannot be empty.", "Invalid name", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                input = makeUniqueName(input);
                break;
            }
            // create local recipe immediately for responsive UI
            Recipe r = new Recipe(nextId(), input);
            recipes.add(r);
            refreshRecipeListModel();
            selectRecipe(r);
            // autosave new recipe locally
            autoSaveActiveRecipe();



            //PYTHON STUFF
            // Call Python createRecipe asynchronously and update recipe_ID with returned id if valid
            CompletableFuture<Integer> fut = PyBridgeInvoker.createRecipe(input);
            String finalInput = input;
            fut.thenAccept(pyId -> {
                if (pyId != null && pyId > 0) {
                    SwingUtilities.invokeLater(() -> {
                        r.recipe_ID = pyId;
                        recomputeAndStore(r);
                        saveRecipes(CUSTOM_RECIPE_FILE);
                        refreshRecipeListModel();
                    });
                } else {
                    System.err.println("Python createRecipe returned invalid id for recipe '" + finalInput + "'");
                }
            });
        });

        deleteBtn.addActionListener(e -> {
            Recipe sel = recipeJList.getSelectedValue();
            if (sel != null && JOptionPane.showConfirmDialog(this, "Delete recipe \"" + sel.recipe_name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                recipes.removeIf(rr -> rr.recipe_ID == sel.recipe_ID);
                saveRecipes(CUSTOM_RECIPE_FILE);
                activeRecipe = null;
                refreshRecipeListModel();
                clearEditor(nameField, descArea);
            }
        });

        recipeJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Recipe sel = recipeJList.getSelectedValue();
                SwingUtilities.invokeLater(() -> {
                    suppressDocumentEvents = true;
                    selectRecipe(sel);
                    if (sel != null) {
                        nameField.setText(sel.recipe_name != null ? sel.recipe_name : "");
                        totalPortionsSpinner.setValue(sel.total_portions);
                        edibleDaysSpinner.setValue(sel.edible_days);
                        String instrText = String.join("\n", sel.instructions != null ? sel.instructions : Collections.emptyList());
                        if ((instrText == null || instrText.isEmpty()) && sel.description != null && !sel.description.isEmpty()) instrText = sel.description;
                        descArea.setText(instrText != null ? instrText : "");
                        ingredientTableModel.setIngredients(sel.recipe_ingredients);
                        updateTotals();
                    } else {
                        clearEditor(nameField, descArea);
                    }
                    suppressDocumentEvents = false;
                });
            }
        });

        nameField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                if (suppressDocumentEvents) return;
                SwingUtilities.invokeLater(() -> {
                    if (activeRecipe != null) {
                        String desired = nameField.getText() == null ? "" : nameField.getText().trim();
                        if (desired.isEmpty()) desired = makeUniqueName("Recipe " + activeRecipe.recipe_ID);
                        else desired = makeUniqueNameExcluding(desired, activeRecipe);
                        suppressDocumentEvents = true;
                        activeRecipe.recipe_name = desired;
                        nameField.setText(desired);
                        refreshRecipeListModel();
                        suppressDocumentEvents = false;
                        markDirty(); // autosave
                    }
                });
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        totalPortionsSpinner.addChangeListener(e -> { if (!suppressDocumentEvents) markDirty(); });
        edibleDaysSpinner.addChangeListener(e -> { if (!suppressDocumentEvents) markDirty(); });

        descArea.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                if (suppressDocumentEvents) return;
                SwingUtilities.invokeLater(() -> {
                    if (activeRecipe != null) {
                        String text = descArea.getText();
                        List<String> instr = new ArrayList<>();
                        if (text != null && !text.trim().isEmpty()) {
                            String[] parts = text.split("\\r?\\n");
                            for (String p : parts) if (!p.trim().isEmpty()) instr.add(p.trim());
                        }
                        activeRecipe.instructions = instr;
                        activeRecipe.description = text != null ? text : "";
                        markDirty(); // autosave
                    }
                });
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        addIngredientBtn.addActionListener(e -> {
            Page2.IngredientSelectionListener listener = json -> { SwingUtilities.invokeLater(() -> handleSelectionFromPage2(json)); };
            Page2 page2 = new Page2(listener, true);
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog mini = new JDialog(owner, "Search Kroger", Dialog.ModalityType.APPLICATION_MODAL);
            mini.getContentPane().setLayout(new BorderLayout());
            mini.add(page2, BorderLayout.CENTER);
            mini.setSize(700, 600);
            mini.setLocationRelativeTo(this);
            mini.setVisible(true);
        });

        customIngredientBtn.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            CustomIngredientDialog dlg = new CustomIngredientDialog(owner, custom -> {
                IngredientEntry ie = new IngredientEntry();
                ie.name = custom.name;
                ie.serving_label = custom.serving_label != null && !custom.serving_label.isEmpty() ? custom.serving_label : "1";
                ie.price_per_serving = custom.price_per_serving;
                ie.calories_per_serving = custom.calories_per_serving;
                ie.unit = custom.unit != null ? custom.unit : "";
                ie.multiplier = 1.0;
                if (activeRecipe == null) {
                    Recipe r = new Recipe(nextId(), "New recipe");
                    recipes.add(r);
                    selectRecipe(r);
                    refreshRecipeListModel();
                }
                if (activeRecipe != null) {
                    activeRecipe.recipe_ingredients.add(ie);
                    ingredientTableModel.setIngredients(activeRecipe.recipe_ingredients);
                    updateTotals();
                    markDirty(); // autosave



                    // PYTHON STUFF
                    // then notify Python the ingredient was added to the recipe.
                    CompletableFuture<Integer> createFut = PyBridgeInvoker.createIngredient(ie.toJson().toString());
                    createFut.thenAccept(ingId -> {
                        if (ingId != null && ingId > 0) {
                            SwingUtilities.invokeLater(() -> {
                                ie.external_id = String.valueOf(ingId);
                                ie.lastSyncedQuantity = ie.quantity; // mark synced
                                recomputeAndStore(activeRecipe);
                                saveRecipes(CUSTOM_RECIPE_FILE);
                                refreshRecipeListModel();
                            });
                        } else {
                            System.err.println("Python ingredientID returned invalid id for ingredient '" + ie.name + "'");
                        }
                        // Always notify and ask Python to link (fire-and-forget)
                        try {
                            // only call addIngredientToRecipe if recipe has server-side id and parsed ingId > 0
                            int recipeId = activeRecipe != null ? activeRecipe.recipe_ID : -1;
                            if (recipeId > 0 && ingId != null && ingId > 0) {
                                PyBridgeInvoker.addIngredientToRecipe(recipeId, ingId, ie.quantity);
                            }
                            PyBridgeInvoker.notifyRecipeAddedIngredient(activeRecipe.toJson().toString(), ie.toJson().toString());
                        } catch (Throwable t) {
                            System.err.println("Failed to notify Python of added ingredient: " + t.getMessage());
                        }
                    });
                }
            });
            dlg.setVisible(true);
        });

        editAmountsBtn.addActionListener(e -> {
            int row = ingredientTable.getSelectedRow();
            if (row >= 0) editIngredientAmounts(row);
            else JOptionPane.showMessageDialog(this, "Select an ingredient row to edit its amounts.");
        });

        viewInfoBtn.addActionListener(e -> {
            int row = ingredientTable.getSelectedRow();
            if (row >= 0) {
                IngredientEntry ie = ingredientTableModel.getIngredients().get(row);
                JSONObject json = ie.toJson();
                NutritionDialog.showNutritionDialog(this, json);
            } else JOptionPane.showMessageDialog(this, "Select an ingredient row to view info.");
        });

        deleteIngredientBtn.addActionListener(e -> {
            int row = ingredientTable.getSelectedRow();
            if (row >= 0) {
                IngredientEntry ie = ingredientTableModel.getIngredients().get(row);
                int confirm = JOptionPane.showConfirmDialog(this, "Remove " + ie.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    ingredientTableModel.getIngredients().remove(row);
                    ingredientTableModel.fireTableRowsDeleted(row, row);
                    updateTotals();
                    markDirty(); // autosave
                }
            } else JOptionPane.showMessageDialog(this, "Select an ingredient to delete.");
        });

        ingredientTable.getSelectionModel().addListSelectionListener(e -> {
            boolean has = ingredientTable.getSelectedRow() >= 0;
            editAmountsBtn.setEnabled(has);
            viewInfoBtn.setEnabled(has);
            deleteIngredientBtn.setEnabled(has);
        });

        ingredientTableModel.addTableModelListener(evt -> { updateTotals(); markDirty(); });

        SwingUtilities.invokeLater(() -> {
            try {
                ingredientTable.getColumnModel().getColumn(2).setCellEditor(new SpinnerEditor(new SpinnerNumberModel(1.0, 0.0, 1000.0, 0.25)));
                ingredientTable.getColumnModel().getColumn(3).setCellEditor(new SpinnerEditor(new SpinnerNumberModel(1.0, 0.0, 10000.0, 1.0)));
            } catch (Exception ignored) {}
        });
    }

    private void clearEditor(JTextField nameField, JTextArea descArea) {
        suppressDocumentEvents = true;
        nameField.setText("");
        descArea.setText("");
        ingredientTableModel.setIngredients(new ArrayList<>());
        totalsLabel.setText("Cost $0.00  |  Calories 0.0 kcal");
        activeRecipe = null;
        totalPortionsSpinner.setValue(1);
        edibleDaysSpinner.setValue(3);
        suppressDocumentEvents = false;
        markClean();
    }

    private void selectRecipe(Recipe r) {
        this.activeRecipe = r;
        if (r != null) {
            ingredientTableModel.setIngredients(r.recipe_ingredients);
            totalPortionsSpinner.setValue(r.total_portions);
            edibleDaysSpinner.setValue(r.edible_days);
        } else ingredientTableModel.setIngredients(new ArrayList<>());
        updateTotals();
    }

    private int nextId() { return recipes.stream().mapToInt(r -> r.recipe_ID).max().orElse(0) + 1; }

    private void refreshRecipeListModel() {
        int selId = activeRecipe != null ? activeRecipe.recipe_ID : -1;
        recipeListModel.clear();
        for (Recipe r : recipes) recipeListModel.addElement(r);
        recipeJList.setCellRenderer(new RecipeListCellRenderer());
        if (selId != -1) {
            for (int i = 0; i < recipeListModel.size(); i++) {
                if (recipeListModel.get(i).recipe_ID == selId) {
                    recipeJList.setSelectedIndex(i);
                    recipeJList.ensureIndexIsVisible(i);
                    break;
                }
            }
        }
    }

    private void recomputeAndStore(Recipe r) {
        double totalCost = 0.0;
        double totalCalories = 0.0;
        for (IngredientEntry ie : r.recipe_ingredients) {
            totalCost += ie.price_per_serving * ie.multiplier;
            totalCalories += ie.calories_per_serving * ie.multiplier;
        }
        r.recipe_cos_sum = totalCost;
        r.cost_cook = totalCost;
        r.cost_per_serving = r.total_portions > 0 ? totalCost / r.total_portions : totalCost;
    }

    private void updateTotals() {
        List<IngredientEntry> ingr = ingredientTableModel.getIngredients();
        double totalCost = 0.0;
        double totalCalories = 0.0;
        for (IngredientEntry ie : ingr) {
            totalCost += ie.price_per_serving * ie.multiplier;
            totalCalories += ie.calories_per_serving * ie.multiplier;
        }
        totalsLabel.setText(String.format("Cost $%.2f  |  Calories %.1f kcal", totalCost, totalCalories));
    }

    // --------------------------------------
    // Page2 selection handling & Kroger fallback
    // --------------------------------------
    private void handleSelectionFromPage2(JSONObject json) {
        try {
            IngredientEntry ie = new IngredientEntry();
            ie.name = json.optString("name", json.optString("description", "Unknown"));
            ie.info = json.optString("info", json.optString("description", ""));
            ie.imagePath = json.optString("imagePath", json.optString("image_url", ""));
            ie.external_id = json.optString("external_id", json.optString("productId", json.optString("id", "")));
            ie.serving_label = json.optString("serving_label", json.optString("serving", ""));
            ie.price_per_serving = json.optDouble("price_per_serving", json.optDouble("price", 0.0));
            ie.calories_per_serving = json.optDouble("calories_per_serving", 0.0);

            if (json.has("nutrients") && !json.isNull("nutrients")) {
                Object nut = json.opt("nutrients");
                if (nut instanceof JSONObject) ie.nutrients_per_serving = (JSONObject) nut;
                else if (nut instanceof JSONArray) {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("items", nut);
                    ie.nutrients_per_serving = wrapper;
                }
            }
            if (json.has("kroger_raw") && !json.isNull("kroger_raw")) ie.kroger_raw = json.optJSONObject("kroger_raw");

            JSONObject raw = json.optJSONObject("kroger_raw") != null ? json.optJSONObject("kroger_raw") : json;

            if (ie.calories_per_serving <= 0.0) {
                double c = extractCaloriesFromRaw(raw);
                if (c <= 0.0) c = extractCaloriesFromNutrientsObject(json);
                if (c > 0.0) ie.calories_per_serving = c;
            }

            if (ie.serving_label == null || ie.serving_label.isEmpty()) {
                String fallback = extractServingLabelFromRaw(raw);
                if (!fallback.isEmpty()) ie.serving_label = fallback;
            }
            if (ie.price_per_serving <= 0.0) {
                double p = raw.optDouble("price", ie.price_per_serving);
                if (p > 0.0) ie.price_per_serving = p;
            }

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6,6,6,6);
            gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel("Serving:"), gc);
            gc.gridx = 1;
            JTextField servingField = new JTextField(ie.serving_label != null && !ie.serving_label.isEmpty() ? ie.serving_label : "1", 20);
            panel.add(servingField, gc);

            gc.gridx = 0; gc.gridy = 1;
            panel.add(new JLabel("Multiplier (how many servings to add):"), gc);
            gc.gridx = 1;
            SpinnerNumberModel spModel = new SpinnerNumberModel(1.0, 0.0, 1000.0, 0.25);
            JSpinner multSpinner = new JSpinner(spModel);
            panel.add(multSpinner, gc);

            gc.gridx = 0; gc.gridy = 2;
            panel.add(new JLabel("Detected calories per serving:"), gc);
            gc.gridx = 1;
            JLabel calsLabel = new JLabel(String.format("%.1f kcal", ie.calories_per_serving));
            panel.add(calsLabel, gc);

            Window owner = SwingUtilities.getWindowAncestor(this);
            int res = JOptionPane.showConfirmDialog(owner, panel, "Serving and options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String newServing = servingField.getText().trim();
            if (!newServing.isEmpty()) ie.serving_label = newServing;
            double mult = ((Number) multSpinner.getValue()).doubleValue();
            if (mult <= 0) mult = 1.0;
            ie.multiplier = mult;

            ie.quantity = json.optDouble("quantity", ie.multiplier);

            if (activeRecipe == null) {
                Recipe r = new Recipe(nextId(), "New recipe");
                recipes.add(r);
                selectRecipe(r);
                refreshRecipeListModel();
            }
            if (activeRecipe != null) {
                activeRecipe.recipe_ingredients.add(ie);
                ingredientTableModel.setIngredients(activeRecipe.recipe_ingredients);
                updateTotals();
                markDirty(); // autosave

                // PYTHON STUFF
                // then notify Python the ingredient was added to the recipe.
                CompletableFuture<Integer> createFut = PyBridgeInvoker.createIngredient(ie.toJson().toString());
                createFut.thenAccept(ingId -> {
                    if (ingId != null && ingId > 0) {
                        SwingUtilities.invokeLater(() -> {
                            ie.external_id = String.valueOf(ingId);
                            ie.lastSyncedQuantity = ie.quantity; // mark as synced
                            recomputeAndStore(activeRecipe);
                            saveRecipes(CUSTOM_RECIPE_FILE);
                            refreshRecipeListModel();
                        });
                    } else {
                        System.err.println("Python ingredientID returned invalid id for ingredient '" + ie.name + "'");
                    }
                    // Always notify and ask Python to link (fire-and-forget)
                    try {
                        // only call addIngredientToRecipe if recipe has server-side id and parsed ingId > 0
                        int recipeId = activeRecipe != null ? activeRecipe.recipe_ID : -1;
                        if (recipeId > 0 && ingId != null && ingId > 0) {
                            PyBridgeInvoker.addIngredientToRecipe(recipeId, ingId, ie.quantity);
                        }
                        PyBridgeInvoker.notifyRecipeAddedIngredient(activeRecipe.toJson().toString(), ie.toJson().toString());
                    } catch (Throwable t) {
                        System.err.println("Failed to notify Python of added ingredient: " + t.getMessage());
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to add ingredient: " + ex.getMessage());
        }
    }

    private JSONObject fetchProductDetailFromBridge(String name, String externalId) {
        try {
            IKrogerWrapper pythonEntry = Py4JHelper.getWrapper();
            if (pythonEntry == null) return null;
            try {
                if (externalId != null && !externalId.isEmpty()) {
                    String raw = pythonEntry.search(externalId, 1);
                    if (raw != null && !raw.isEmpty()) {
                        JSONArray arr = new JSONArray(raw);
                        if (arr.length() > 0) return arr.getJSONObject(0);
                    }
                }
            } catch (Throwable ignored) {}
            try {
                String raw = pythonEntry.search(name, 1);
                if (raw != null && !raw.isEmpty()) {
                    JSONArray arr = new JSONArray(raw);
                    if (arr.length() > 0) return arr.getJSONObject(0);
                }
            } catch (Throwable ex) {}
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // =========================================================================
    // Calories extraction helper
    // =========================================================================
    public static double extractCaloriesFromNutrientsObject(JSONObject obj) {
        if (obj == null) return 0.0;

        Object nutrients = obj.opt("nutrients");
        if (nutrients instanceof JSONArray) {
            double c = extractCaloriesFromNutrientsArray((JSONArray) nutrients);
            if (c > 0.0) return c;
        } else if (nutrients instanceof JSONObject) {
            JSONObject nobj = (JSONObject) nutrients;
            double c = nobj.optDouble("calories", -1.0);
            if (c >= 0.0) return c;
            Object inner = nobj.opt("items");
            if (inner instanceof JSONArray) {
                c = extractCaloriesFromNutrientsArray((JSONArray) inner);
                if (c > 0.0) return c;
            }
        }

        Object niObj = obj.opt("nutritionInformation");
        JSONObject nutri = null;
        if (niObj instanceof JSONObject) nutri = (JSONObject) niObj;
        else if (niObj instanceof JSONArray && ((JSONArray) niObj).length() > 0) nutri = ((JSONArray) niObj).optJSONObject(0);
        if (nutri != null) {
            Object n2 = nutri.opt("nutrients");
            if (n2 instanceof JSONArray) {
                double c = extractCaloriesFromNutrientsArray((JSONArray) n2);
                if (c > 0.0) return c;
            }
        }

        double top = obj.optDouble("calories", -1.0);
        if (top >= 0.0) return top;
        top = obj.optDouble("calories_per_serving", -1.0);
        if (top >= 0.0) return top;

        return 0.0;
    }

    private static double extractCaloriesFromNutrientsArray(JSONArray arr) {
        if (arr == null) return 0.0;
        final Pattern numberPat = Pattern.compile("(\\d+(?:\\.\\d+)?)");

        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.opt(i);
            if (item instanceof JSONObject) {
                JSONObject n = (JSONObject) item;
                String name = n.optString("displayName", n.optString("name", "")).toLowerCase();
                String code = n.optString("code", "").toLowerCase();
                if (name.contains("calor") || "calories".equals(code) || "cal".equals(code)) {
                    double q = n.optDouble("quantity", Double.NaN);
                    if (!Double.isNaN(q) && q >= 0.0) return q;
                    String qtyText = n.optString("quantity", n.optString("value", n.optString("label", "")));
                    if (qtyText != null && !qtyText.isEmpty()) {
                        Matcher m = numberPat.matcher(qtyText);
                        if (m.find()) {
                            try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                        }
                    }
                }
            } else if (item instanceof String) {
                String s = (String) item;
                String lower = s.toLowerCase();
                if (lower.contains("calor")) {
                    Matcher m = numberPat.matcher(s);
                    if (m.find()) {
                        try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                } else {
                    Matcher m = numberPat.matcher(s);
                    if (m.find() && (s.toLowerCase().contains("calor") || s.toLowerCase().contains("calories"))) {
                        try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                }
            }
        }
        return 0.0;
    }

    public static double extractCaloriesFromRaw(JSONObject raw) {
        double c = extractCaloriesFromNutrientsObject(raw);
        if (c > 0.0) return c;
        if (raw == null) return 0.0;
        double topLevel = raw.optDouble("calories", -1.0);
        if (topLevel >= 0.0) return topLevel;
        topLevel = raw.optDouble("calories_per_serving", -1.0);
        if (topLevel >= 0.0) return topLevel;
        return 0.0;
    }

    public static String extractServingLabelFromRaw(JSONObject raw) {
        if (raw == null) return "";
        String s = raw.optString("serving_label", "");
        if (!s.isEmpty()) return s;
        s = raw.optString("serving", "");
        if (!s.isEmpty()) return s;

        Object niObj = raw.opt("nutritionInformation");
        JSONObject nutri = null;
        if (niObj instanceof JSONObject) nutri = (JSONObject) niObj;
        else if (niObj instanceof JSONArray && ((JSONArray) niObj).length() > 0) nutri = ((JSONArray) niObj).optJSONObject(0);

        if (nutri != null) {
            Object servObj = nutri.opt("servingSize");
            JSONObject serv = null;
            if (servObj instanceof JSONObject) serv = (JSONObject) servObj;
            else if (servObj instanceof JSONArray && ((JSONArray) servObj).length() > 0) serv = ((JSONArray) servObj).optJSONObject(0);
            if (serv != null) {
                double qty = Double.NaN;
                if (serv.has("quantity")) qty = serv.optDouble("quantity", Double.NaN);
                else if (serv.has("value")) qty = serv.optDouble("value", Double.NaN);
                String unit = "";
                Object uom = serv.opt("unitOfMeasure");
                if (uom instanceof JSONObject) unit = ((JSONObject) uom).optString("abbreviation", ((JSONObject) uom).optString("name", ""));
                else if (uom instanceof String) unit = (String) uom;
                if (!Double.isNaN(qty)) {
                    String qtyStr = (qty % 1.0 == 0.0) ? Integer.toString((int) qty) : Double.toString(qty);
                    return (qtyStr + " " + unit).trim();
                }
            }
        }

        JSONArray items = raw.optJSONArray("items");
        if (items != null && items.length() > 0) {
            JSONObject item = items.optJSONObject(0);
            if (item != null) {
                String[] keys = new String[] { "size", "netContent", "packageSize", "displaySize", "sizeDescription", "measure", "packageSizeDescription" };
                for (String k : keys) {
                    String val = item.optString(k, "");
                    if (val != null && !val.isEmpty()) return val;
                }
                JSONArray sizes = item.optJSONArray("sizes");
                if (sizes != null && sizes.length() > 0) {
                    JSONObject s0 = sizes.optJSONObject(0);
                    if (s0 != null) {
                        String label = s0.optString("size", s0.optString("name", ""));
                        if (label != null && !label.isEmpty()) return label;
                    }
                }
            }
        }

        String[] textCandidates = new String[] { raw.optString("description", ""), raw.optString("receiptDescription", ""), raw.optString("displayName", ""), raw.optString("name", "") };
        for (String txt : textCandidates) {
            if (txt == null || txt.isEmpty()) continue;
            Matcher m = SERVING_REGEX.matcher(txt);
            if (m.find()) {
                String qty = m.group(1);
                String unit = m.group(2);
                return qty + " " + unit;
            }
        }

        return "";
    }

    // --------------------------------------
    // Loading / Saving recipes
    // --------------------------------------
    public List<Recipe> loadRecipes(String path) {
        List<Recipe> list = new ArrayList<>();
        File f = new File(path);
        if (!f.exists()) {
            legacyLoadFromTSV();
            return new ArrayList<>(this.recipes);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            String txt = sb.toString().trim();
            if (txt.isEmpty()) return list;

            try {
                JSONArray arr = new JSONArray(txt);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Recipe r = Recipe.fromJson(o);
                    list.add(r);
                }
                this.recipes.clear();
                this.recipes.addAll(list);
                return list;
            } catch (Exception je) {

            }
        } catch (Exception e) {

        }

        legacyLoadFromTSV();
        return new ArrayList<>(this.recipes);
    }

    private void legacyLoadFromTSV() {
        File f = new File(CUSTOM_RECIPE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t");
                if (p.length < 9) continue;
                Recipe r = new Recipe();
                r.recipe_ID = safeParseInt(p[0]);
                r.recipe_name = p.length > 1 ? p[1] : "";
                r.recipe_cos_sum = p.length > 2 ? safeParseDouble(p[2]) : 0.0;
                r.cost_cook = p.length > 3 ? safeParseDouble(p[3]) : 0.0;
                r.cost_per_serving = p.length > 4 ? safeParseDouble(p[4]) : 0.0;
                r.cart_cost = p.length > 5 ? safeParseDouble(p[5]) : 0.0;
                r.nutrition_grade = p.length > 6 ? p[6] : "";
                r.description = p.length > 7 ? p[7] : "";
                r.imagePath = p.length > 8 ? p[8] : "";
                try {
                    if (p.length > 9 && p[9] != null && !p[9].isEmpty()) {
                        String ingrJson = new String(Base64.getDecoder().decode(p[9]));
                        JSONArray arr = new JSONArray(ingrJson);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            IngredientEntry ie = IngredientEntry.fromJson(o);
                            r.recipe_ingredients.add(ie);
                        }
                    }
                } catch (Exception ignored) {}
                recipes.add(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveRecipes(String path) {
        try {
            File f = new File(path);
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            JSONArray root = new JSONArray();
            for (Recipe r : recipes) root.put(r.toJson());
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(root.toString(2));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save recipes: " + e.getMessage());
        }
    }

    private static int safeParseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double safeParseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }

    private class RecipeListCellRenderer extends JLabel implements ListCellRenderer<Recipe> {
        RecipeListCellRenderer() { setOpaque(true); setBorder(BorderFactory.createEmptyBorder(4,6,4,6)); }
        public Component getListCellRendererComponent(JList<? extends Recipe> list, Recipe value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.recipe_name != null ? value.recipe_name : ("Recipe " + value.recipe_ID));
            setBackground(isSelected ? new Color(200,220,255) : Color.WHITE);
            return this;
        }
    }

    private class IngredientTableModel extends AbstractTableModel {

        private final String[] cols = new String[] { "Name", "Serving", "Multiplier", "Qty", "Unit", "Price/serving", "Total cost", "Calories/serving", "Total cal", "Actions" };
        private List<IngredientEntry> data = new ArrayList<>();

        public void setIngredients(List<IngredientEntry> ing) { this.data = new ArrayList<>(ing); fireTableDataChanged(); }
        public List<IngredientEntry> getIngredients() { return data; }

        public void add(IngredientEntry ie) { data.add(ie); fireTableRowsInserted(data.size()-1, data.size()-1); }

        public void commitEdits() { if (ingredientTable.getCellEditor() != null) ingredientTable.getCellEditor().stopCellEditing(); }

        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public boolean isCellEditable(int row, int col) { return col == 2 || col == 3; }

        public Object getValueAt(int r, int c) {
            IngredientEntry ie = data.get(r);
            switch (c) {
                case 0: return ie.name;
                case 1: return ie.serving_label != null && !ie.serving_label.isEmpty() ? ie.serving_label : "(unknown)";
                case 2: return ie.multiplier;
                case 3: return ie.quantity;
                case 4: return ie.unit;
                case 5: return String.format("$%.2f", ie.price_per_serving);
                case 6: return String.format("$%.2f", ie.price_per_serving * ie.multiplier);
                case 7: return String.format("%.1f kcal", ie.calories_per_serving);
                case 8: return String.format("%.1f kcal", ie.calories_per_serving * ie.multiplier);
                case 9: return "Remove";
                default: return "";
            }
        }

        public void setValueAt(Object value, int r, int c) {
            IngredientEntry ie = data.get(r);
            if (c == 2) ie.multiplier = safeParseDouble(String.valueOf(value));
            else if (c == 3) ie.quantity = safeParseDouble(String.valueOf(value));
            fireTableRowsUpdated(r, r);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ingredientTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = ingredientTable.rowAtPoint(e.getPoint());
                int col = ingredientTable.columnAtPoint(e.getPoint());
                if (row < 0) return;
                IngredientEntry ie = ingredientTableModel.getIngredients().get(row);
                if (col == 9) {
                    int confirm = JOptionPane.showConfirmDialog(Page4.this, "Remove " + ie.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        ingredientTableModel.getIngredients().remove(row);
                        ingredientTableModel.fireTableRowsDeleted(row, row);
                        updateTotals();
                        markDirty(); // autosave
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    editIngredientAmounts(row);
                }
            }
        });
    }

    private void editIngredientAmounts() {
        int row = ingredientTable.getSelectedRow();
        if (row >= 0) {
            editIngredientAmounts(row);
        } else {
            JOptionPane.showMessageDialog(this, "Select an ingredient row to edit its amounts.");
        }
    }

    // allow editing multiplier/quantity
    private void editIngredientAmounts(int row) {
        IngredientEntry ie = ingredientTableModel.getIngredients().get(row);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Serving:"), gc);
        gc.gridx = 1;
        JTextField servingField = new JTextField(ie.serving_label != null && !ie.serving_label.isEmpty() ? ie.serving_label : "1", 20);
        panel.add(servingField, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Multiplier:"), gc);
        gc.gridx = 1;
        SpinnerNumberModel m1 = new SpinnerNumberModel(ie.multiplier, 0.0, 1000.0, 0.25);
        JSpinner s1 = new JSpinner(m1);
        panel.add(s1, gc);

        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gc);
        gc.gridx = 1;
        SpinnerNumberModel m2 = new SpinnerNumberModel(ie.quantity, 0.0, 100000.0, 1.0);
        JSpinner s2 = new JSpinner(m2);
        panel.add(s2, gc);

        Window owner = SwingUtilities.getWindowAncestor(this);
        int res = JOptionPane.showConfirmDialog(owner, panel, "Edit amounts", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String newServing = servingField.getText().trim();
        if (!newServing.isEmpty()) ie.serving_label = newServing;
        ie.multiplier = ((Number) s1.getValue()).doubleValue();
        ie.quantity = ((Number) s2.getValue()).doubleValue();
        ingredientTableModel.fireTableRowsUpdated(row, row);
        updateTotals();
        markDirty(); // autosave
    }

    private static class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;
        SpinnerEditor(SpinnerNumberModel model) { spinner = new JSpinner(model); ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setBorder(null); }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            try { double v = Double.parseDouble(String.valueOf(value)); spinner.setValue(v); } catch (Exception ignored) {}
            return spinner;
        }
        public Object getCellEditorValue() { return spinner.getValue(); }
    }

    private String makeUniqueName(String base) {
        if (base == null) base = "Recipe";
        String candidate = base.trim();
        if (candidate.isEmpty()) candidate = "Recipe";
        Set<String> existing = new HashSet<>();
        for (Recipe r : recipes) {
            if (r.recipe_name != null) existing.add(r.recipe_name.toLowerCase());
        }
        if (!existing.contains(candidate.toLowerCase())) return candidate;
        int i = 1;
        String next;
        do {
            next = candidate + " (" + i + ")";
            i++;
        } while (existing.contains(next.toLowerCase()));
        return next;
    }

    private String makeUniqueNameExcluding(String base, Recipe excluding) {
        if (base == null) base = "Recipe";
        String candidate = base.trim();
        if (candidate.isEmpty()) candidate = "Recipe";
        Set<String> existing = new HashSet<>();
        for (Recipe r : recipes) {
            if (r == excluding) continue;
            if (r.recipe_name != null) existing.add(r.recipe_name.toLowerCase());
        }
        if (!existing.contains(candidate.toLowerCase())) return candidate;
        int i = 1;
        String next;
        do {
            next = candidate + " (" + i + ")";
            i++;
        } while (existing.contains(next.toLowerCase()));
        return next;
    }
}