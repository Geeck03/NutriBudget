package pages;

import bridge.IKrogerWrapper;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class SuggestionsPage extends JPanel {

    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color DARK_GRAY = new Color(45, 45, 45);

    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private IKrogerWrapper krogerWrapper; // Optional: fetch product prices
    private Connection dbConnection; // MySQL connection

    private JComboBox<String> priceFilterDropdown;
    private JComboBox<String> nutriScoreFilter;

    private List<Recipe> allRecipes;

    public SuggestionsPage(Connection dbConnection, IKrogerWrapper krogerWrapper) {
        this.dbConnection = dbConnection;
        this.krogerWrapper = krogerWrapper;

        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Recipe Suggestions", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(DARK_GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(LIGHT_GRAY);
        titlePanel.add(title, BorderLayout.CENTER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(titlePanel, BorderLayout.NORTH);

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        filterPanel.setBackground(LIGHT_GRAY);

        // Max Price dropdown
        filterPanel.add(new JLabel("Max Price ($):"));
        String[] priceOptions = {"No Limit", "5", "10", "15", "20", "25", "30"};
        priceFilterDropdown = new JComboBox<>(priceOptions);
        priceFilterDropdown.setSelectedIndex(0);
        filterPanel.add(priceFilterDropdown);

        // NutriScore filter
        filterPanel.add(new JLabel("NutriScore:"));
        nutriScoreFilter = new JComboBox<>(new String[]{"A", "B", "C", "D", "E"});
        nutriScoreFilter.setSelectedIndex(0);
        filterPanel.add(nutriScoreFilter);

        JButton applyFilterButton = new JButton("Apply Filters");
        filterPanel.add(applyFilterButton);

        add(filterPanel, BorderLayout.NORTH);

        // Grid panel
        gridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Load recipes
        SwingUtilities.invokeLater(() -> {
            allRecipes = fetchRecipesFromDB();
            updateGrid();
        });

        // Filter action
        applyFilterButton.addActionListener(e -> updateGrid());
    }

    // Apply filters and refresh grid
    private void updateGrid() {
        gridPanel.removeAll();

        double maxPrice = Double.MAX_VALUE;
        String minNutriScore = (String) nutriScoreFilter.getSelectedItem();

        // Get selected max price from dropdown
        String selectedPrice = (String) priceFilterDropdown.getSelectedItem();
        if (selectedPrice != null && !selectedPrice.equals("No Limit")) {
            try {
                maxPrice = Double.parseDouble(selectedPrice);
            } catch (NumberFormatException ignored) {}
        }

        for (Recipe r : allRecipes) {
            double totalPrice = r.totalPrice();
            String recipeNutriScore = r.worstNutriScore();
            if (totalPrice <= maxPrice && compareNutriScores(recipeNutriScore, minNutriScore) <= 0) {
                gridPanel.add(createRecipeCard(r));
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // Returns -1 if score1 < score2, 0 if equal, 1 if score1 > score2 (A < B < C < D < E)
    private int compareNutriScores(String score1, String score2) {
        String order = "ABCDE";
        return Integer.compare(order.indexOf(score1), order.indexOf(score2));
    }

    private List<Recipe> fetchRecipesFromDB() {
        List<Recipe> recipes = new ArrayList<>();
        String query = "SELECT recipe_ID, recipe_name, instructions FROM Recipes";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("recipe_ID");
                String name = rs.getString("recipe_name");
                String instructions = rs.getString("instructions");

                List<Ingredient> ingredients = fetchIngredientsForRecipe(id);
                recipes.add(new Recipe(id, name, instructions, ingredients));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recipes;
    }

    private List<Ingredient> fetchIngredientsForRecipe(int recipeId) {
        List<Ingredient> ingredients = new ArrayList<>();
        String query = "SELECT I.ingredient_ID, I.ingredient_name, I.calories, I.protein, I.carbs, I.fats, I.nutriscore, I.price " +
                       "FROM RecipeIngredients RI " +
                       "JOIN Ingredients I ON RI.ingredient_ID = I.ingredient_ID " +
                       "WHERE RI.recipe_ID = ?";

        try (PreparedStatement ps = dbConnection.prepareStatement(query)) {
            ps.setInt(1, recipeId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ingredients.add(new Ingredient(
                        rs.getInt("ingredient_ID"),
                        rs.getString("ingredient_name"),
                        rs.getDouble("calories"),
                        rs.getDouble("protein"),
                        rs.getDouble("carbs"),
                        rs.getDouble("fats"),
                        rs.getString("nutriscore"),
                        rs.getDouble("price")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients;
    }

    private JPanel createRecipeCard(Recipe r) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(220, 200));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel name = new JLabel("<html><center>" + r.name + "</center></html>");
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea ingredientsArea = new JTextArea();
        ingredientsArea.setText(r.ingredientsString() + "\nPrice: $" + String.format("%.2f", r.totalPrice()) +
                "\nNutriScore: " + r.worstNutriScore());
        ingredientsArea.setWrapStyleWord(true);
        ingredientsArea.setLineWrap(true);
        ingredientsArea.setEditable(false);
        ingredientsArea.setBackground(Color.WHITE);

        card.add(name);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(ingredientsArea);

        return card;
    }

    // Inner classes
    private static class Recipe {
        int id;
        String name;
        String instructions;
        List<Ingredient> ingredients;

        public Recipe(int id, String name, String instructions, List<Ingredient> ingredients) {
            this.id = id;
            this.name = name;
            this.instructions = instructions;
            this.ingredients = ingredients;
        }

        public String ingredientsString() {
            StringBuilder sb = new StringBuilder();
            for (Ingredient i : ingredients) {
                sb.append(i.name).append(", ");
            }
            return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
        }

        public double totalPrice() {
            double sum = 0;
            for (Ingredient i : ingredients) sum += i.price;
            return sum;
        }

        public String worstNutriScore() {
            String worst = "A";
            String order = "ABCDE";
            for (Ingredient i : ingredients) {
                if (order.indexOf(i.nutriscore) > order.indexOf(worst)) worst = i.nutriscore;
            }
            return worst;
        }
    }

    private static class Ingredient {
        int id;
        String name;
        double calories;
        double protein;
        double carbs;
        double fats;
        String nutriscore;
        double price;

        public Ingredient(int id, String name, double calories, double protein, double carbs, double fats, String nutriscore, double price) {
            this.id = id;
            this.name = name;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fats = fats;
            this.nutriscore = nutriscore;
            this.price = price;
        }
    }
}
