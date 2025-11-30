package pages;

import bridge.IKrogerWrapper;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

//=====================================================================
// SuggestionsPage
//=====================================================================
public class SuggestionsPage extends JPanel {

    private final Connection dbConnection;
    private final IKrogerWrapper krogerWrapper;

    private List<Recipe> recipes = new ArrayList<>();
    private JPanel gridPanel;
    private JScrollPane scrollPane;

    private JComboBox<String> gradeFilter;
    private JTextField minPriceField;
    private JTextField maxPriceField;
    private JButton filterButton;

    // NutriScore ordering
    private static final String GRADES = "ABCDE";

    //==============================
    // Recipe class
    //==============================
    public static class Recipe {
        public int id;
        public String name;
        public double ingredientCostSum;
        public double costCook;
        public double costPerServing;
        public double cartCost;
        public String nutritionGrade;
        public List<Ingredient> ingredients;

        public Recipe(int id, String name, double ingredientCostSum, double costCook, double costPerServing,
                      double cartCost, String nutritionGrade, List<Ingredient> ingredients) {
            this.id = id;
            this.name = name;
            this.ingredientCostSum = ingredientCostSum;
            this.costCook = costCook;
            this.costPerServing = costPerServing;
            this.cartCost = cartCost;
            this.nutritionGrade = nutritionGrade != null ? nutritionGrade : "Unknown";
            this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        }

        // Compute total ingredient cost dynamically
        public double totalIngredientCost() {
            return ingredientCostSum;
        }

        // Worst NutriScore among ingredients
        public String worstNutriScore() {
            String worst = "A";
            for (Ingredient ing : ingredients) {
                if (GRADES.indexOf(ing.nutriScore) > GRADES.indexOf(worst)) worst = ing.nutriScore;
            }
            return worst;
        }
    }

    //==============================
    // Ingredient class
    //==============================
    public static class Ingredient {
        public String name;
        public double price;
        public String nutriScore;

        public Ingredient(String name, double price, String nutriScore) {
            this.name = name;
            this.price = price;
            this.nutriScore = nutriScore != null ? nutriScore : "A";
        }
    }

    //==============================
    // Constructor
    //==============================
    public SuggestionsPage(Connection dbConnection, IKrogerWrapper krogerWrapper) {
        this.dbConnection = dbConnection;
        this.krogerWrapper = krogerWrapper;

        setLayout(new BorderLayout());

        buildUI();
        loadRecipesFromDB();
        refreshGrid();
    }

    //==============================
    // Build UI
    //==============================
    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        gradeFilter = new JComboBox<>(new String[]{"All", "A", "B", "C", "D", "E"});
        minPriceField = new JTextField(5);
        maxPriceField = new JTextField(5);
        filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> refreshGrid());

        topPanel.add(new JLabel("Grade:"));
        topPanel.add(gradeFilter);
        topPanel.add(new JLabel("Min $:"));
        topPanel.add(minPriceField);
        topPanel.add(new JLabel("Max $:"));
        topPanel.add(maxPriceField);
        topPanel.add(filterButton);

        add(topPanel, BorderLayout.NORTH);

        gridPanel = new JPanel();
        gridPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    //==============================
    // Load recipes from DB
    //==============================
    private void loadRecipesFromDB() {
        recipes.clear();

        if (dbConnection == null) {
            System.out.println("⚠ DB connection is null, cannot load recipes.");
            return;
        }

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT recipe_ID, recipe_name, ingredient_cost_sum, cost_cook, cost_per_serving, cart_cost, nutrition_grade " +
                             "FROM Recipes"
             )) {

            while (rs.next()) {
                int id = rs.getInt("recipe_ID");
                String name = rs.getString("recipe_name");
                double ingCost = rs.getDouble("ingredient_cost_sum");
                double costCook = rs.getDouble("cost_cook");
                double costPerServ = rs.getDouble("cost_per_serving");
                double cartCost = rs.getDouble("cart_cost");
                String grade = rs.getString("nutrition_grade");

                // For worstNutriScore, use dummy ingredient list (replace with real ingredients if available)
                List<Ingredient> ingredients = new ArrayList<>();
                ingredients.add(new Ingredient("Dummy", ingCost, grade != null ? grade : "A"));

                recipes.add(new Recipe(id, name, ingCost, costCook, costPerServ, cartCost, grade, ingredients));
            }

            System.out.println("Loaded recipes: " + recipes.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //==============================
    // Refresh grid (filter by grade and price)
    //==============================
    private void refreshGrid() {
        String selectedGrade = gradeFilter.getSelectedItem().toString();
        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;

        try {
            if (!minPriceField.getText().trim().isEmpty())
                minPrice = Double.parseDouble(minPriceField.getText().trim());
        } catch (NumberFormatException ignored) {}

        try {
            if (!maxPriceField.getText().trim().isEmpty())
                maxPrice = Double.parseDouble(maxPriceField.getText().trim());
        } catch (NumberFormatException ignored) {}

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : recipes) {
            boolean matchesGrade = selectedGrade.equals("All") || r.worstNutriScore().equalsIgnoreCase(selectedGrade);
            boolean matchesPrice = r.totalIngredientCost() >= minPrice && r.totalIngredientCost() <= maxPrice;

            if (matchesGrade && matchesPrice) filtered.add(r);
        }

        gridPanel = buildGridPanelForRecipes(filtered);
        scrollPane.setViewportView(gridPanel);
        revalidate();
        repaint();
    }

    //==============================
    // Build grid panel
    //==============================
    private JPanel buildGridPanelForRecipes(List<Recipe> items) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(Color.WHITE);

        for (Recipe r : items) {
            grid.add(createRecipeCard(r));
        }

        return grid;
    }

    //==============================
    // Create recipe card
    //==============================
    private JPanel createRecipeCard(Recipe r) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setBackground(new Color(250, 250, 250));
        card.setPreferredSize(new Dimension(180, 180));

        JLabel name = new JLabel(r.name, SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel grade = new JLabel(r.worstNutriScore(), SwingConstants.CENTER);
        grade.setAlignmentX(Component.CENTER_ALIGNMENT);
        grade.setOpaque(true);
        grade.setForeground(Color.WHITE);
        grade.setFont(new Font("SansSerif", Font.BOLD, 16));
        grade.setPreferredSize(new Dimension(50, 25));
        grade.setHorizontalAlignment(SwingConstants.CENTER);

        switch (r.worstNutriScore().toUpperCase()) {
            case "A": grade.setBackground(new Color(0, 153, 0)); break;
            case "B": grade.setBackground(new Color(153, 204, 0)); break;
            case "C": grade.setBackground(new Color(255, 204, 0)); break;
            case "D": grade.setBackground(new Color(255, 153, 51)); break;
            case "E": grade.setBackground(new Color(204, 0, 0)); break;
            default: grade.setBackground(Color.GRAY);
        }

        JLabel price = new JLabel(String.format("$%.2f", r.totalIngredientCost()), SwingConstants.CENTER);
        price.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(8));
        card.add(name);
        card.add(Box.createVerticalStrut(8));
        card.add(grade);
        card.add(Box.createVerticalStrut(8));
        card.add(price);

        return card;
    }
}
