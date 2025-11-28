package pages;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;
import bridge.IKrogerWrapper;
import bridge.Py4JHelper;


public class Page2 extends JPanel {

    public interface IngredientSelectionListener {
        void onIngredientSelected(JSONObject json);
    }

    private final IngredientSelectionListener listener;
    private final boolean miniMode;
    private final JTextField searchField = new JTextField();
    private final JButton searchBtn = new JButton("Search");
    private final DefaultListModel<JSONObject> resultsModel = new DefaultListModel<>();
    private final JList<JSONObject> resultsList = new JList<>(resultsModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultListModel<JSONObject> favoritesModel = new DefaultListModel<>();
    private final JList<JSONObject> favoritesList = new JList<>(favoritesModel);
    private final DefaultListModel<CustomIngredient> customModel = new DefaultListModel<>();
    private final JList<CustomIngredient> customList = new JList<>(customModel);
    private JPanel customPanel;
    private static final String FAVORITES_FILE = "src/pages/text/favorite_products.json";
    private static final int IMAGE_CACHE_SIZE = 64;
    private static final Map<String, ImageIcon> imageCache = new LinkedHashMap<>(IMAGE_CACHE_SIZE, 0.75f, true) {

        protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) { return size() > IMAGE_CACHE_SIZE; }
    };

    private static final ImageIcon PLACEHOLDER = (ImageIcon) UIManager.getIcon("FileView.fileIcon");
    private static final int THUMB = 80;
    private static final Pattern SERVING_REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(g|kg|mg|ml|l|oz|fl\\s?oz|cup|cups|tbsp|tsp|lb|lbs)\\b", Pattern.CASE_INSENSITIVE);

    public Page2() { this(json -> { /* no-op */ }, false); }

    public Page2(IngredientSelectionListener listener, boolean miniMode) {
        this.listener = listener != null ? listener : (json -> {});
        this.miniMode = miniMode;
        buildUI();
        loadFavorites();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search", buildSearchPanel());
        tabs.addTab("Favorites", buildFavoritesPanel());


        if (!miniMode) tabs.addTab("Examples", buildExamplesPanel());

        customPanel = buildCustomPanel();
        tabs.addTab("Custom", customPanel);


        tabs.addChangeListener(e -> {
            Component sel = tabs.getSelectedComponent();
            if (sel == customPanel) refreshCustomList();
        });

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));
        JPanel top = new JPanel(new BorderLayout(6,6));
        searchField.setColumns(30);
        searchField.addActionListener(e -> doSearch());
        top.add(searchField, BorderLayout.CENTER);
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBtn.addActionListener(e -> doSearch());
        rightTop.add(searchBtn);
        top.add(rightTop, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        resultsList.setCellRenderer(new ResultCellRenderer());
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(resultsList);
        panel.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6,6));
        bottom.add(statusLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton favBtn = new JButton("Favorite");
        favBtn.addActionListener(e -> addSelectedToFavorites());
        actions.add(favBtn);

        JButton infoBtn = new JButton("View Info");
        infoBtn.addActionListener(e -> viewSelectedInfo(false));
        actions.add(infoBtn);

        JButton addToRecipeBtn = new JButton("Add to recipe");
        addToRecipeBtn.setVisible(miniMode); // only show in popup (mini mode)
        addToRecipeBtn.setEnabled(false);
        addToRecipeBtn.setToolTipText("Add selected ingredient to recipe and close");
        addToRecipeBtn.addActionListener(e -> addSelectedToRecipeAndClose());
        actions.add(addToRecipeBtn);

        bottom.add(actions, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        // Enable/disable Add button when selection changes
        resultsList.addListSelectionListener(e -> {
            boolean hasSel = resultsList.getSelectedIndex() >= 0;
            addToRecipeBtn.setEnabled(hasSel && miniMode);
        });

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (miniMode) addSelectedToRecipeAndClose();
                    else viewSelectedInfo(false);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = resultsList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        resultsList.setSelectedIndex(idx);
                        showResultContextMenu(resultsList, e.getX(), e.getY());
                    }
                }
            }
        });

        return panel;
    }

    private JPanel buildFavoritesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        favoritesList.setCellRenderer(new ResultCellRenderer());
        favoritesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(favoritesList);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeBtn = new JButton("Remove favorite");
        removeBtn.addActionListener(e -> removeSelectedFavorite());
        JButton viewBtn = new JButton("View Info");
        viewBtn.addActionListener(e -> viewSelectedFavoriteInfo());
        bottom.add(removeBtn);
        bottom.add(viewBtn);

        // Add-to-recipe button for favorites (visible only in miniMode)
        JButton favAddToRecipeBtn = new JButton("Add to recipe");
        favAddToRecipeBtn.setVisible(miniMode);
        favAddToRecipeBtn.setEnabled(false);
        favAddToRecipeBtn.setToolTipText("Add selected favorite to recipe and close");
        favAddToRecipeBtn.addActionListener(e -> addSelectedFavoriteToRecipeAndClose());
        bottom.add(favAddToRecipeBtn);

        panel.add(bottom, BorderLayout.SOUTH);

        favoritesList.addListSelectionListener(e -> {
            boolean hasSel = favoritesList.getSelectedIndex() >= 0;
            favAddToRecipeBtn.setEnabled(hasSel && miniMode);
        });

        favoritesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (miniMode) addSelectedFavoriteToRecipeAndClose();
                    else viewSelectedFavoriteInfo();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = favoritesList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        favoritesList.setSelectedIndex(idx);
                        showFavoritesContextMenu(favoritesList, e.getX(), e.getY());
                    }
                }
            }
        });

        return panel;
    }

    private JPanel buildExamplesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        DefaultListModel<JSONObject> examplesModel = new DefaultListModel<>();
        JList<JSONObject> examplesList = new JList<>(examplesModel);
        examplesList.setCellRenderer(new ResultCellRenderer());
        examplesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(examplesList);
        panel.add(scroll, BorderLayout.CENTER);


        List<Page4.Recipe> recipes = RecipeLoader.loadRecipesFromFile("src/pages/text/recipes.txt");
        for (Page4.Recipe r : recipes) {
            JSONObject jo = new JSONObject();
            jo.put("name", r.recipe_name != null ? r.recipe_name : "");
            if (r.imagePath != null && !r.imagePath.isEmpty()) jo.put("image_url", r.imagePath);
            jo.put("serving_label", "");
            jo.put("price", r.cost_per_serving);
            jo.put("kroger_raw", r.toJson());
            examplesModel.addElement(jo);
        }

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Add to recipe");
        addBtn.setVisible(miniMode);
        addBtn.setEnabled(false);
        addBtn.addActionListener(e -> {
            JSONObject sel = examplesList.getSelectedValue();
            if (sel != null) addToRecipeAndClose(sel);
        });
        bottom.add(addBtn);
        JButton infoBtn = new JButton("View Info");
        infoBtn.addActionListener(e -> {
            JSONObject sel = examplesList.getSelectedValue();
            if (sel != null) showNutritionDialog(sel);
        });
        bottom.add(infoBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        examplesList.addListSelectionListener(e -> addBtn.setEnabled(examplesList.getSelectedIndex() >= 0 && miniMode));

        examplesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (miniMode) addToRecipeAndClose(examplesList.getSelectedValue());
                    else showNutritionDialog(examplesList.getSelectedValue());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = examplesList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        examplesList.setSelectedIndex(idx);
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem info = new JMenuItem("View Info");
                        info.addActionListener(a -> showNutritionDialog(examplesList.getSelectedValue()));
                        menu.add(info);
                        menu.show(examplesList, e.getX(), e.getY());
                    }
                }
            }
        });

        return panel;
    }

    private JPanel buildCustomPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        customList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object value, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, value, idx, sel, focus);
                if (value instanceof CustomIngredient) setText(((CustomIngredient) value).toString());
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(customList);
        panel.add(sp, BorderLayout.CENTER);


        refreshCustomList();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton manageBtn = new JButton("Manage...");
        JButton addBtn = new JButton("Add to recipe");
        JButton infoBtn = new JButton("View Info");
        JButton refreshBtn = new JButton("Refresh");
        addBtn.setVisible(miniMode);
        addBtn.setEnabled(false);

        manageBtn.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            CustomIngredientDialog dlg = new CustomIngredientDialog(owner, null);
            dlg.setVisible(true);
            refreshCustomList();
        });

        addBtn.addActionListener(e -> {
            CustomIngredient sel = customList.getSelectedValue();
            if (sel != null) {
                JSONObject out = new JSONObject();
                out.put("name", sel.name);
                out.put("serving_label", sel.serving_label != null ? sel.serving_label : "");
                out.put("price_per_serving", sel.price_per_serving);
                out.put("calories_per_serving", sel.calories_per_serving);
                out.put("unit", sel.unit != null ? sel.unit : "unit");
                // do NOT include recipe-level fields here
                addToRecipeAndClose(out);
            }
        });

        infoBtn.addActionListener(e -> {
            CustomIngredient sel = customList.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }
            String txt = String.format("Name: %s%nServing: %s%nUnit: %s%nPrice/serv: $%.2f%nCalories/serv: %.1f%nNotes: %s",
                    sel.name, sel.serving_label, sel.unit, sel.price_per_serving, sel.calories_per_serving, sel.notes);
            JOptionPane.showMessageDialog(this, txt, "Custom ingredient", JOptionPane.INFORMATION_MESSAGE);
        });

        refreshBtn.addActionListener(e -> refreshCustomList());

        bottom.add(manageBtn);
        bottom.add(infoBtn);
        bottom.add(refreshBtn);
        bottom.add(addBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        customList.addListSelectionListener(e -> addBtn.setEnabled(customList.getSelectedIndex() >= 0 && miniMode));
        customList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (miniMode && customList.getSelectedValue() != null) addBtn.doClick();
                    else infoBtn.doClick();
                }
            }
        });

        return panel;
    }

    private void refreshCustomList() {
        customModel.clear();
        List<CustomIngredient> items = CustomIngredientStore.loadAll();
        for (CustomIngredient ci : items) customModel.addElement(ci);
    }

    private void setStatus(String s) { statusLabel.setText(s); }

    private void doSearch() {
        final String q = searchField.getText().trim();
        if (q.isEmpty()) { setStatus("Enter a search term."); return; }
        searchBtn.setEnabled(false);
        resultsModel.clear();
        setStatus("Searching...");

        SwingWorker<JSONArray, Void> worker = new SwingWorker<>() {
            @Override protected JSONArray doInBackground() throws Exception {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper == null) throw new IllegalStateException("Kroger bridge unavailable");
                String raw = wrapper.search(q, 10);
                if (raw == null || raw.trim().isEmpty()) return new JSONArray();
                return new JSONArray(raw);
            }
            @Override protected void done() {
                try {
                    JSONArray arr = get();
                    for (int i = 0; i < arr.length(); i++) resultsModel.addElement(arr.getJSONObject(i));
                    setStatus(String.format("Found %d results", arr.length()));
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    setStatus("Search failed: " + (msg != null ? msg : "unknown error"));
                } finally { searchBtn.setEnabled(true); }
            }
        };
        worker.execute();
    }

    private void addSelectedToFavorites() {
        JSONObject sel = resultsList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select item to favorite."); return; }
        addFavorite(sel);
    }

    private void removeSelectedFavorite() {
        JSONObject sel = favoritesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a favorite to remove."); return; }
        favoritesModel.removeElement(sel);
        saveFavorites();
    }

    private void viewSelectedInfo(boolean fromFavorites) {
        JSONObject sel = resultsList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }
        showNutritionDialog(sel);
    }

    private void viewSelectedFavoriteInfo() {
        JSONObject sel = favoritesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a favorite first."); return; }
        showNutritionDialog(sel);
    }


    private void addSelectedToRecipeAndClose() {
        addToRecipeAndClose(resultsList.getSelectedValue());
    }

    // Add from favorites
    private void addSelectedFavoriteToRecipeAndClose() {
        addToRecipeAndClose(favoritesList.getSelectedValue());
    }


    private void addToRecipeAndClose(JSONObject selected) {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an ingredient first."); return; }

        JSONObject out = buildSelectionJson(selected);


        if (selected.has("kroger_raw") && !selected.isNull("kroger_raw")) {
            out.put("kroger_raw", selected.opt("kroger_raw"));
        } else {
            out.put("kroger_raw", selected);
        }

        if (selected.has("nutrients") && !selected.isNull("nutrients")) {
            out.put("nutrients", selected.opt("nutrients"));
        }

        JSONObject raw = out.optJSONObject("kroger_raw") != null ? out.optJSONObject("kroger_raw") : out;
        if (out.optString("serving_label","").isEmpty()) {
            String fallback = Page4.extractServingLabelFromRaw(raw);
            if (!fallback.isEmpty()) out.put("serving_label", fallback);
        }
        if (out.optDouble("calories_per_serving",0.0) <= 0.0) {
            double c = Page4.extractCaloriesFromRaw(raw);
            if (c > 0.0) out.put("calories_per_serving", c);
            else {
                double c2 = Page4.extractCaloriesFromNutrientsObject(selected);
                if (c2 > 0.0) out.put("calories_per_serving", c2);
            }
        }

        try { listener.onIngredientSelected(out); }
        catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Failed to deliver selection: " + ex.getMessage()); return; }

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
    }

    private JSONObject buildSelectionJson(JSONObject selected) {
        JSONObject out = new JSONObject();
        out.put("name", selected.optString("name", selected.optString("description", "Unknown")));
        out.put("info", selected.optString("describe", selected.optString("description", "")));
        out.put("imagePath", selected.optString("image_url", selected.optString("imagePath", "")));
        out.put("external_id", selected.optString("id", selected.optString("productId", "")));
        out.put("productId", selected.optString("id", selected.optString("productId", "")));
        out.put("price_per_serving", selected.optDouble("price", selected.optDouble("price_per_serving", 0.0)));
        out.put("price", selected.optDouble("price", selected.optDouble("price_per_serving", 0.0)));
        out.put("serving_label", selected.optString("serving_label", selected.optString("serving", "")));
        out.put("calories_per_serving", selected.optDouble("calories_per_serving", 0.0));
        if (selected.has("nutrients") && !selected.isNull("nutrients")) out.put("nutrients", selected.opt("nutrients"));
        out.put("kroger_raw", selected);
        out.put("quantity", selected.optDouble("quantity", 0.0));
        out.put("unit", selected.optString("unit", "unit"));
        // do not attach recipe-level fields here
        return out;
    }

    private void showResultContextMenu(JComponent parentList, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem info = new JMenuItem("View information");
        info.addActionListener(e -> viewSelectedInfo(false));
        JMenuItem raw = new JMenuItem("Show raw JSON");
        raw.addActionListener(e -> showSelectedRawJson());
        JMenuItem fav = new JMenuItem("Add to favorites");
        fav.addActionListener(e -> {
            JSONObject sel = resultsList.getSelectedValue();
            if (sel != null) addFavorite(sel);
        });
        menu.add(info);
        menu.add(raw);
        menu.add(fav);
        menu.show(parentList, x, y);
    }

    private void showFavoritesContextMenu(JComponent parentList, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem info = new JMenuItem("View information");
        info.addActionListener(e -> viewSelectedFavoriteInfo());
        JMenuItem raw = new JMenuItem("Show raw JSON");
        raw.addActionListener(e -> showSelectedRawJsonFavorites());
        JMenuItem remove = new JMenuItem("Remove favorite");
        remove.addActionListener(e -> removeSelectedFavorite());
        menu.add(info);
        menu.add(raw);
        menu.add(remove);
        menu.show(parentList, x, y);
    }

    private void showSelectedRawJson() {
        JSONObject sel = resultsList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }
        JTextArea ta = new JTextArea(sel.toString(2));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, sp, "Raw product JSON", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSelectedRawJsonFavorites() {
        JSONObject sel = favoritesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a favorite first."); return; }
        JTextArea ta = new JTextArea(sel.toString(2));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, sp, "Raw favorite JSON", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showNutritionDialog(JSONObject sel) {
        NutritionDialog.showNutritionDialog(this, sel);
    }


    private void addFavorite(JSONObject product) {
        String id = product.optString("id", product.optString("productId", ""));
        for (int i = 0; i < favoritesModel.size(); i++) {
            JSONObject e = favoritesModel.get(i);
            String eid = e.optString("id", e.optString("productId", ""));
            if (eid.equals(id)) { JOptionPane.showMessageDialog(this, "Already in favorites."); return; }
        }
        favoritesModel.addElement(product);
        saveFavorites();
    }

    private void loadFavorites() {
        favoritesModel.clear();
        File f = new File(FAVORITES_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            String txt = sb.toString().trim();
            if (txt.isEmpty()) return;
            JSONArray arr = new JSONArray(txt);
            for (int i = 0; i < arr.length(); i++) favoritesModel.addElement(arr.getJSONObject(i));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void saveFavorites() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < favoritesModel.size(); i++) arr.put(favoritesModel.get(i));
        File f = new File(FAVORITES_FILE);
        try {
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(f, false)) { fw.write(arr.toString(2)); }
        } catch (IOException ex) { ex.printStackTrace(); }
    }


    private static String extractServingLabelFromRaw(JSONObject raw) {
        return Page4.extractServingLabelFromRaw(raw);
    }

    private static double extractCaloriesFromRaw(JSONObject raw) {
        return Page4.extractCaloriesFromRaw(raw);
    }
}