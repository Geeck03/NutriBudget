package pages;




import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import static java.awt.Component.LOCK;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import bridge.Py4JHelper;
import pages.CustomIngredientDialog;
import py4j.ClientServer;
import py4j.Py4JNetworkException;

import org.json.JSONArray;
import org.json.JSONObject;




import bridge.IKrogerWrapper;
import bridge.Py4JHelper;




//======================================================================================================================
// Unified Page2: Kroger Ingredients + DB Recipes
//======================================================================================================================




public class Page2 extends JPanel {




  //==============================================================================================================
  // Listener interface
  //==============================================================================================================
  public interface IngredientSelectionListener {
      void onIngredientSelected(JSONObject json);
  }




  private final IngredientSelectionListener listener;
  private final boolean miniMode;




  //==============================================================================================================
  // Ingredients (Kroger API)
  //==============================================================================================================
  private final JTextField searchField = new JTextField();
  private final JButton searchBtn = new JButton("Search");
  private final DefaultListModel<JSONObject> resultsModel = new DefaultListModel<>();
  private final JList<JSONObject> resultsList = new JList<>(resultsModel);
  private final DefaultListModel<JSONObject> favoritesModel = new DefaultListModel<>();
  private final JList<JSONObject> favoritesList = new JList<>(favoritesModel);
  private final DefaultListModel<CustomIngredient> customModel = new DefaultListModel<>();
  private final JList<CustomIngredient> customList = new JList<>(customModel);
  private JPanel customPanel;
  private final JLabel statusLabel = new JLabel(" ");




  private static final String FAVORITES_FILE = "src/pages/text/favorite_products.json";
  private static final int IMAGE_CACHE_SIZE = 64;
  private static final Map<String, ImageIcon> imageCache = new LinkedHashMap<>(IMAGE_CACHE_SIZE, 0.75f, true) {
      protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) { return size() > IMAGE_CACHE_SIZE; }
  };
private static final ImageIcon PLACEHOLDER;
private static final String CUSTOM_RECIPE_FILE = "src/pages/text/custom_recipes.txt";








static {
  Icon icon = UIManager.getIcon("FileView.fileIcon");
  if (icon instanceof ImageIcon) {
      PLACEHOLDER = (ImageIcon) icon;
  } else {
      // convert Icon to BufferedImage → ImageIcon
      int w = icon.getIconWidth();
      int h = icon.getIconHeight();
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics g = img.createGraphics();
      icon.paintIcon(null, g, 0, 0);
      g.dispose();
      PLACEHOLDER = new ImageIcon(img);
  }
}








  private static final int THUMB = 80;
  private static final Pattern SERVING_REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(g|kg|mg|ml|l|oz|fl\\s?oz|cup|cups|tbsp|tsp|lb|lbs)\\b", Pattern.CASE_INSENSITIVE);




  //==============================================================================================================
  // Recipes (DB)
  //==============================================================================================================
// Recipe UI references
private JPanel recipesPanel;  // the "Recipes" tab panel
private JPanel recipesGrid;   // the grid inside the panel that holds cards


  public static class Recipe {
   public int id;
   public String name;
   public int totalPortions;
   public double costPerServing;
   public double costCook;
   public double cartCost;
   public String nutritionGrade;
   public String instructions; // you can convert JSONArray to string
   public String imagePath;


   public List<JSONObject> ingredients; // optional, keep raw for now


   public Recipe(JSONObject obj) {
       this.id = obj.optInt("id");
       this.name = obj.optString("name");
       this.totalPortions = obj.optInt("total_portions", 1);
       this.costPerServing = obj.optDouble("cost_per_serving", 0.0);
       this.costCook = obj.optDouble("cost_cook", 0.0);
       this.cartCost = obj.optDouble("cart_cost", 0.0);
       this.nutritionGrade = obj.optString("nutrition_grade", "");
       this.imagePath = obj.optString("imagePath", "");
       JSONArray instrArr = obj.optJSONArray("instructions");
       this.instructions = instrArr != null ? instrArr.toString() : "";
       JSONArray ingrArr = obj.optJSONArray("ingredients");
       this.ingredients = new ArrayList<>();
       if (ingrArr != null) for (int i = 0; i < ingrArr.length(); i++) this.ingredients.add(ingrArr.getJSONObject(i));
   }
}




  private final List<Recipe> recipes = new ArrayList<>();




  // DB connection info
  private final String dbUrl = String.format(
          "jdbc:mysql://%s:3306/%s",
          System.getenv("host"),
          System.getenv("database")
  );
  private final String dbUser = System.getenv("user");
  private final String dbPassword = System.getenv("password");




  //==============================================================================================================
  // Constructor
  //==============================================================================================================
  public Page2() { this(json -> {}, false); }




  public Page2(IngredientSelectionListener listener, boolean miniMode) {
      this.listener = listener != null ? listener : json -> {};
      this.miniMode = miniMode;




      buildUI();
      loadFavorites();
      loadRecipesFromFile();
  }




  //==============================================================================================================
  // UI BUILD
  //==============================================================================================================
  private void buildUI() {
      setLayout(new BorderLayout());
      JTabbedPane tabs = new JTabbedPane();




      tabs.addTab("Search", buildSearchPanel());
      tabs.addTab("Favorites", buildFavoritesPanel());
      if (!miniMode) tabs.addTab("Custom", buildCustomPanel());
      tabs.addTab("Recipes", buildRecipesPanel());




      tabs.addChangeListener(e -> {
          Component sel = tabs.getSelectedComponent();
          if (sel == customPanel) refreshCustomList();
      });




      add(tabs, BorderLayout.CENTER);
  }




  //--------------------------------------------------------------------------------------------------------------
  // Search panel
  //--------------------------------------------------------------------------------------------------------------
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
      addToRecipeBtn.setVisible(miniMode);
      addToRecipeBtn.setEnabled(false);
      addToRecipeBtn.setToolTipText("Add selected ingredient to recipe and close");
      addToRecipeBtn.addActionListener(e -> addSelectedToRecipeAndClose());
      actions.add(addToRecipeBtn);




      bottom.add(actions, BorderLayout.EAST);
      panel.add(bottom, BorderLayout.SOUTH);




      resultsList.addListSelectionListener(e -> addToRecipeBtn.setEnabled(resultsList.getSelectedIndex() >= 0 && miniMode));
      resultsList.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
              if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                  if (miniMode) addSelectedToRecipeAndClose();
                  else viewSelectedInfo(false);
              } else if (SwingUtilities.isRightMouseButton(e)) {
                  int idx = resultsList.locationToIndex(e.getPoint());
                  if (idx >= 0) resultsList.setSelectedIndex(idx);
              }
          }
      });




      return panel;
  }




  //--------------------------------------------------------------------------------------------------------------
  // Favorites panel
  //--------------------------------------------------------------------------------------------------------------
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




      JButton favAddToRecipeBtn = new JButton("Add to recipe");
      favAddToRecipeBtn.setVisible(miniMode);
      favAddToRecipeBtn.setEnabled(false);
      favAddToRecipeBtn.setToolTipText("Add selected favorite to recipe and close");
      favAddToRecipeBtn.addActionListener(e -> addSelectedFavoriteToRecipeAndClose());
      bottom.add(favAddToRecipeBtn);




      panel.add(bottom, BorderLayout.SOUTH);




      favoritesList.addListSelectionListener(e -> favAddToRecipeBtn.setEnabled(favoritesList.getSelectedIndex() >= 0 && miniMode));
      favoritesList.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
              if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                  if (miniMode) addSelectedFavoriteToRecipeAndClose();
                  else viewSelectedFavoriteInfo();
              } else if (SwingUtilities.isRightMouseButton(e)) {
                  int idx = favoritesList.locationToIndex(e.getPoint());
                  if (idx >= 0) favoritesList.setSelectedIndex(idx);
              }
          }
      });




      return panel;
  }




  //--------------------------------------------------------------------------------------------------------------
  // Custom panel
  //--------------------------------------------------------------------------------------------------------------
  private JPanel buildCustomPanel() {
      customPanel = new JPanel(new BorderLayout(8,8));
      customPanel.setBorder(new EmptyBorder(8,8,8,8));




      customList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      customList.setCellRenderer(new DefaultListCellRenderer() {
          public Component getListCellRendererComponent(JList<?> l, Object value, int idx, boolean sel, boolean focus) {
              super.getListCellRendererComponent(l, value, idx, sel, focus);
              if (value instanceof CustomIngredient) setText(((CustomIngredient) value).toString());
              return this;
          }
      });




      JScrollPane sp = new JScrollPane(customList);
      customPanel.add(sp, BorderLayout.CENTER);




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
      customPanel.add(bottom, BorderLayout.SOUTH);




      customList.addListSelectionListener(e -> addBtn.setEnabled(customList.getSelectedIndex() >= 0 && miniMode));
      customList.addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent e) {
              if (e.getClickCount() == 2) {
                  if (miniMode && customList.getSelectedValue() != null) addBtn.doClick();
                  else infoBtn.doClick();
              }
          }
      });




      return customPanel;
  }




  private void refreshCustomList() {
      customModel.clear();
      List<CustomIngredient> items = CustomIngredientStore.loadAll();
      for (CustomIngredient ci : items) customModel.addElement(ci);
  }




  //==============================================================================================================
  // Recipes panel
  //==============================================================================================================
private JPanel buildRecipesPanel() {
   recipesPanel = new JPanel(new BorderLayout(8,8));
   recipesPanel.setBorder(new EmptyBorder(8,8,8,8));


   recipesGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
   recipesGrid.setBackground(Color.WHITE);


   JScrollPane scroll = new JScrollPane(recipesGrid);
   recipesPanel.add(scroll, BorderLayout.CENTER);


   return recipesPanel;
}
private void refreshRecipesGrid() {
   if (recipesGrid == null) return;   // safety check
   recipesGrid.removeAll();
   for (Recipe r : recipes) {
       recipesGrid.add(createRecipeCard(r));
   }
   recipesGrid.revalidate();  // re-layout the panel
   recipesGrid.repaint();     // redraw it
}




  private JPanel createRecipeCard(Recipe r) {
      JPanel card = new JPanel();
      card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
      card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
              BorderFactory.createEmptyBorder(10, 10, 10, 10)
      ));
      card.setBackground(new Color(250, 250, 250));
      card.setPreferredSize(new Dimension(180, 180));




      JLabel name = new JLabel(r.name, SwingConstants.CENTER);
      name.setFont(new Font("SansSerif", Font.BOLD, 14));
      name.setAlignmentX(Component.CENTER_ALIGNMENT);




      JLabel image = new JLabel();
      image.setAlignmentX(Component.CENTER_ALIGNMENT);
      image.setPreferredSize(new Dimension(120, 90));




      if (r.imagePath != null && !r.imagePath.isEmpty()) {
          try {
              ImageIcon icon = new ImageIcon(new URL(r.imagePath));
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




      card.addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
              showRecipeDialog(r);
          }
      });




      return card;
  }




  private void showRecipeDialog(Recipe r) {
int numIngredients = r.ingredients != null ? r.ingredients.size() : 0;

String info = String.format(
    "Recipe: %s%nIngredients: %d%nCost/Serving: $%.2f%nNutrition Grade: %s%nInstructions: %s",
    r.name, numIngredients, r.costPerServing, r.nutritionGrade, r.instructions
);

      JOptionPane.showMessageDialog(this, info, "Recipe Info", JOptionPane.INFORMATION_MESSAGE);
  }




  //==============================================================================================================
  // DB: Load recipes
  //==============================================================================================================
//==============================================================================================================
// Load recipes from custom_recipes.txt
//==============================================================================================================
private void loadRecipesFromFile() {
   recipes.clear();
   try {
       String content = new String(Files.readAllBytes(Paths.get(CUSTOM_RECIPE_FILE)), StandardCharsets.UTF_8);
       JSONArray arr = new JSONArray(content);


       for (int i = 0; i < arr.length(); i++) {
           JSONObject json = arr.getJSONObject(i);
           recipes.add(new Recipe(json));
       }


       System.out.println("Loaded recipes: " + recipes.size());


       refreshRecipesGrid();  // <<< THIS IS NEW
   } catch (IOException e) {
       System.out.println("No custom recipes file found: " + CUSTOM_RECIPE_FILE);
   } catch (Exception e) {
       e.printStackTrace();
   }
}


  //==============================================================================================================
  // Kroger API Search
  //==============================================================================================================
public static void connectToExistingBridge(String host, int port) {
    synchronized (LOCK) {
        if (client != null) return; // already connected
        try {
            client = new ClientServer.ClientServerBuilder()
                    .javaPort(0)        // any available local port
                    .pythonPort(port)   // your Python bridge port
                    .build();
            System.out.println("✅ Connected to existing Python bridge at " + host + ":" + port);
        } catch (Py4JNetworkException ex) {
            System.err.println("Failed to connect to Python bridge: " + ex.getMessage());
            client = null;
        } catch (Throwable t) {
            t.printStackTrace();
            client = null;
        }
    }
}


  private void doSearch() {


   // In constructor after Py4JHelper is initialized




      final String q = searchField.getText().trim();
      if (q.isEmpty()) {
          setStatus("Enter a search term.");
          return;
      }
      searchBtn.setEnabled(false);
      resultsModel.clear();
      setStatus("Searching...");




      SwingWorker<JSONArray, Void> worker = new SwingWorker<>() {
        @Override
        protected JSONArray doInBackground() {
            List<JSONObject> list = loadIngredientsFromPython(q);
            return new JSONArray(list);
        }

          @Override
          protected void done() {
              try {
                  JSONArray arr = get();
                  for (int i = 0; i < arr.length(); i++) resultsModel.addElement(arr.getJSONObject(i));
                  setStatus(String.format("Found %d results", arr.length()));
              } catch (InterruptedException | ExecutionException ex) {
                  ex.printStackTrace();
                  String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                  setStatus("Search failed: " + (msg != null ? msg : "unknown error"));
              } finally {
                  searchBtn.setEnabled(true);
              }
          }
      };




      worker.execute();
  }
// Call this from somewhere (e.g., constructor) for testing
private void testKrogerSearch(String query) {
   System.out.println("Testing Kroger search for: " + query);
   try {
       IKrogerWrapper wrapper = Py4JHelper.getWrapper();
       if (wrapper == null) {
           System.out.println("Py4JWrapper is null! Bridge not initialized.");
           return;
       }
       String raw = wrapper.search(query, 10);
       if (raw == null || raw.trim().isEmpty()) {
           System.out.println("No results returned from Kroger API.");
       } else {
           System.out.println("Raw JSON result:");
           System.out.println(raw);


           // Optional: parse and print each product name
           JSONArray arr = new JSONArray(raw);
           for (int i = 0; i < arr.length(); i++) {
               JSONObject product = arr.getJSONObject(i);
               System.out.println((i+1) + ". " + product.optString("name", "Unnamed product"));
           }
       }
   } catch (Exception ex) {
       System.out.println("Error during Kroger search:");
       ex.printStackTrace();
   }
}




  //==============================================================================================================
  // Favorites
  //==============================================================================================================
private void addSelectedToFavorites() {
    JSONObject sel = resultsList.getSelectedValue();
    if (sel == null) { 
        JOptionPane.showMessageDialog(this, "Select item to favorite."); 
        return; 
    }
    addFavorite(sel);
}



  private void removeSelectedFavorite() {
      JSONObject sel = favoritesList.getSelectedValue();
      if (sel == null) { JOptionPane.showMessageDialog(this, "Select a favorite to remove."); return; }
      favoritesModel.removeElement(sel);
      saveFavorites();
  }




private void addFavorite(JSONObject product) {
    String id = product.optString("id", null);
    if (id == null) return;

    JSONObject fav = new JSONObject();
    fav.put("productId", id);
    fav.put("name", product.optString("name", product.optString("description", "Unknown")));
    fav.put("description", product.optString("description", ""));
    fav.put("price", product.optDouble("price", 0.0));
    fav.put("image_url", product.optString("image_url", ""));

    // prevent duplicates
    for (int i = 0; i < favoritesModel.size(); i++)
        if (id.equals(favoritesModel.get(i).optString("productId"))) return;

    favoritesModel.addElement(fav);
    saveFavorites();
}





  private void saveFavorites() {
      JSONArray arr = new JSONArray();
      for (int i = 0; i < favoritesModel.size(); i++) arr.put(favoritesModel.get(i));
      try (FileWriter fw = new FileWriter(FAVORITES_FILE)) { fw.write(arr.toString(2)); }
      catch (IOException e) { e.printStackTrace(); }
  }




  private void loadFavorites() {
      favoritesModel.clear();
      try {
          String jsonStr = Files.readString(Paths.get(FAVORITES_FILE));
          JSONArray arr = new JSONArray(jsonStr);
          for (int i = 0; i < arr.length(); i++) favoritesModel.addElement(arr.getJSONObject(i));
      } catch (IOException e) { System.out.println("No favorites file found."); }
  }




  //==============================================================================================================
  // Utilities
  //==============================================================================================================
  private void setStatus(String text) { statusLabel.setText(text); }




  private void addToRecipeAndClose(JSONObject json) {
      listener.onIngredientSelected(json);
      if (miniMode) SwingUtilities.getWindowAncestor(this).dispose();
  }




  private void addSelectedToRecipeAndClose() { addToRecipeAndClose(resultsList.getSelectedValue()); }
  private void addSelectedFavoriteToRecipeAndClose() { addToRecipeAndClose(favoritesList.getSelectedValue()); }




  private void viewSelectedInfo(boolean modal) {
      JSONObject sel = resultsList.getSelectedValue();
      if (sel == null) return;
      JOptionPane.showMessageDialog(this, sel.toString(2), "Ingredient Info", JOptionPane.INFORMATION_MESSAGE);
  }
  private void viewSelectedFavoriteInfo() { viewSelectedInfo(false); }




  //==============================================================================================================
  // Custom ingredient support
  //==============================================================================================================
  public static class CustomIngredient {
      public String name;
      public String unit;
      public String serving_label;
      public double price_per_serving;
      public double calories_per_serving;
      public String notes;
      public CustomIngredient(String name, String unit, String serving_label, double price_per_serving, double calories_per_serving, String notes) {
          this.name = name; this.unit = unit; this.serving_label = serving_label;
          this.price_per_serving = price_per_serving; this.calories_per_serving = calories_per_serving; this.notes = notes;
      }
      @Override public String toString() { return name; }
  }




  public static class CustomIngredientStore {
      public static List<CustomIngredient> loadAll() { return new ArrayList<>(); }
  }




  //==============================================================================================================
  // Cell renderer
  //==============================================================================================================
private static class ResultCellRenderer extends JPanel implements ListCellRenderer<JSONObject> {
    private final JLabel lblImage = new JLabel();
    private final JLabel lblName = new JLabel();
    private final JLabel lblPrice = new JLabel();

    public ResultCellRenderer() {
        setLayout(new BorderLayout(8, 0));

        add(lblImage, BorderLayout.WEST);

        JPanel mid = new JPanel(new BorderLayout());
        mid.add(lblName, BorderLayout.CENTER);
        mid.add(lblPrice, BorderLayout.SOUTH);
        add(mid, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JSONObject> list, JSONObject value, int idx, boolean sel, boolean focus) {
        if (value == null) return this;

        // --- Display name ---
        lblName.setText(value.optString("name", "Unknown"));

        // --- Display price ---
        double price = value.optDouble("price", 0.0);
        lblPrice.setText(String.format("$%.2f", price));

        // --- Handle image ---
// Load image from URL
String rawUrl = value.optString("image_url", "").trim();  // <- use image_url
if (!rawUrl.isEmpty()) {
    if (rawUrl.startsWith("//")) rawUrl = "https:" + rawUrl;
    final String finalUrl = rawUrl;

    // Check cache
    if (imageCache.containsKey(finalUrl)) {
        lblImage.setIcon(scaleIcon(imageCache.get(finalUrl)));
    } else {
        new Thread(() -> {
            try {
                ImageIcon icon = new ImageIcon(new URL(finalUrl));
                // Wait until image is fully loaded
                Image img = icon.getImage();
                MediaTracker tracker = new MediaTracker(new JLabel());
                tracker.addImage(img, 0);
                tracker.waitForAll();

                if (!tracker.isErrorAny()) {
                    ImageIcon scaledIcon = scaleIcon(icon);
                    imageCache.put(finalUrl, scaledIcon);
                    SwingUtilities.invokeLater(() -> lblImage.setIcon(scaledIcon));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> lblImage.setIcon(PLACEHOLDER));
            }
        }).start();
    }
} else {
    lblImage.setIcon(PLACEHOLDER);
}


        setBackground(sel ? new Color(200, 220, 255) : Color.WHITE);
        return this;
    }

    private static ImageIcon scaleIcon(ImageIcon icon) {
        Image img = icon.getImage().getScaledInstance(THUMB, THUMB, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}

  private List<JSONObject> loadIngredientsFromPython(String query) {
    List<JSONObject> list = new ArrayList<>();

    // First, add existing favorites or static ingredients if needed
    for (int i = 0; i < favoritesModel.size(); i++) {
        JSONObject fav = favoritesModel.get(i);
        if (fav.optString("description", "").toLowerCase().contains(query.toLowerCase())) {
            list.add(fav);
        }
    }

    // Then fetch from Kroger API via Py4J
    try {
        IKrogerWrapper wrapper = Py4JHelper.getWrapper();
        if (wrapper != null) {
            String raw = wrapper.search(query, 10);
            if (raw != null && !raw.trim().isEmpty()) {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getJSONObject(i));
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return list;
}
}
