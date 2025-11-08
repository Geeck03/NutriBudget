package pages;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

//======================================================================================================================
// Custom Calendar Panel
//======================================================================================================================
public class CustomCalendarPanel extends JPanel {

    private JLabel monthYearLabel;
    private JPanel calendarPanel;
    private LocalDate currentDate;
    private Map<String, Map<String, List<Page4.Recipe>>> mealPlans = new HashMap<>();
    private static final String MEAL_PLAN_FILE = "src/pages/text/meal_plans.txt";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //==================================================================================================================
    // Constructor
    //==================================================================================================================
    public CustomCalendarPanel() {
        setLayout(new BorderLayout());
        currentDate = LocalDate.now();

        // Header with month navigation
        JPanel headerPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        prevButton.addActionListener(e -> {
            currentDate = currentDate.minusMonths(1);
            refreshCalendar();
        });
        nextButton.addActionListener(e -> {
            currentDate = currentDate.plusMonths(1);
            refreshCalendar();
        });

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthYearLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main calendar container
        calendarPanel = new JPanel(new BorderLayout());
        add(calendarPanel, BorderLayout.CENTER);

        loadAllMealPlans();
        refreshCalendar();
    }

    //==================================================================================================================
    // Calendar Rendering
    //==================================================================================================================
    private void refreshCalendar() {
        calendarPanel.removeAll();

        // Day of the week
        JPanel daysHeader = new JPanel(new GridLayout(1, 7, 5, 5));
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : dayNames) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            dayLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
            daysHeader.add(dayLabel);
        }

        // Main days grid
        JPanel daysGrid = new JPanel(new GridLayout(0, 7, 5, 5));
        daysGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        YearMonth yearMonth = YearMonth.from(currentDate);
        monthYearLabel.setText(yearMonth.getMonth().toString() + " " + yearMonth.getYear());

        LocalDate firstDay = yearMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = yearMonth.lengthOfMonth();

        // Empty cells before the first day of the month
        for (int i = 0; i < startDay; i++) {
            daysGrid.add(new JLabel(""));
        }

        // Add day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFocusPainted(false);
            dayButton.setOpaque(true);

            String dateStr = DATE_FORMAT.format(date);
            boolean hasMealPlan = mealPlans.containsKey(dateStr)
                    && mealPlans.get(dateStr).values().stream().anyMatch(l -> !l.isEmpty());

            if (date.equals(LocalDate.now())) {
                dayButton.setBackground(new Color(173, 216, 230)); // Current day
            } else if (hasMealPlan) {
                dayButton.setBackground(new Color(144, 238, 144)); // Planned day
            } else {
                dayButton.setBackground(Color.WHITE);
            }

            dayButton.addActionListener(e -> openDaySidebar(dateStr, dayButton));
            daysGrid.add(dayButton);
        }

        // Combine day header + grid
        calendarPanel.add(daysHeader, BorderLayout.NORTH);
        calendarPanel.add(daysGrid, BorderLayout.CENTER);

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    //==================================================================================================================
    // Sidebar for Planned Meals
    //==================================================================================================================
    private void openDaySidebar(String dateStr, JButton dayButton) {
        Map<String, List<Page4.Recipe>> dayMeals = mealPlans.getOrDefault(dateStr, new HashMap<>());
        JDialog dialog = new JDialog((Frame) null, "Planned Recipes - " + dateStr, true);
        dialog.setSize(450, 650);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Display each meal type
        for (String mealType : new String[]{"Breakfast", "Lunch", "Dinner", "Snack"}) {
            List<Page4.Recipe> recipes = dayMeals.getOrDefault(mealType, new ArrayList<>());

            JLabel mealLabel = new JLabel(mealType);
            mealLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            mealLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
            contentPanel.add(mealLabel);

            if (recipes.isEmpty()) {
                JLabel noneLabel = new JLabel("No meals selected yet.");
                noneLabel.setForeground(Color.GRAY);
                noneLabel.setBorder(new EmptyBorder(5, 20, 5, 5));
                contentPanel.add(noneLabel);
            }

            for (Page4.Recipe r : recipes) {
                JPanel card = new JPanel(new BorderLayout(15, 10));
                card.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(Color.GRAY, 1)));
                card.setMaximumSize(new Dimension(400, 100));

                JLabel imgLabel = new JLabel(Page4.loadImageIconStatic(r.imagePath, 80, 80));
                card.add(imgLabel, BorderLayout.WEST);

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setBorder(new EmptyBorder(5, 10, 5, 5));
                infoPanel.add(new JLabel(r.name));
                infoPanel.add(new JLabel("Calories: " + r.calories + " | Protein: " + r.protein + "g"));
                infoPanel.add(new JLabel("Carbs: " + r.carbs + " | Cost: $" + r.cost));
                card.add(infoPanel, BorderLayout.CENTER);

                JButton removeBtn = new JButton("Remove");
                removeBtn.addActionListener(e -> {
                    recipes.remove(r);
                    saveMealPlan(dateStr, mealType, recipes);
                    contentPanel.remove(card);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                    refreshCalendar();
                });
                card.add(removeBtn, BorderLayout.EAST);

                contentPanel.add(card);
            }

            // Separator line
            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            contentPanel.add(Box.createVerticalStrut(5));
            contentPanel.add(separator);
            contentPanel.add(Box.createVerticalStrut(5));
        }

        JButton addMealBtn = new JButton("Add Meal");
        addMealBtn.addActionListener(e -> {
            dialog.dispose();
            openMealTypeDialog(LocalDate.parse(dateStr, DATE_FORMAT), dayButton);
        });

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(addMealBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //==================================================================================================================
    // Meal Type Selection
    //==================================================================================================================
    private void openMealTypeDialog(LocalDate date, JButton dayButton) {
        String dateStr = DATE_FORMAT.format(date);
        JDialog dialog = new JDialog((Frame) null, "Select Meal Type", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Snack"};
        for (String meal : meals) {
            JButton btn = new JButton(meal);
            btn.setFont(new Font("SansSerif", Font.BOLD, 16));
            btn.addActionListener(e -> {
                dialog.dispose();
                openMealEditorDialog(dateStr, meal, dayButton);
            });
            buttonPanel.add(btn);
        }

        dialog.add(new JLabel("Select a meal to edit for " + dateStr, SwingConstants.CENTER), BorderLayout.NORTH);
        dialog.add(buttonPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    //==================================================================================================================
    // Meal Editor Dialog
    //==================================================================================================================
    private void openMealEditorDialog(String dateStr, String mealType, JButton dayButton) {
        JDialog dialog = new JDialog((Frame) null, "Edit " + mealType + " for " + dateStr, true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        JPanel recipePanel = new JPanel();
        recipePanel.setLayout(new BoxLayout(recipePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(recipePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        List<Page4.Recipe> allRecipes = Page4.loadRecipesFromFile("src/pages/text/recipes.txt");
        allRecipes.addAll(Page4.loadRecipesFromFile("src/pages/text/custom_recipes.txt"));

        List<Page4.Recipe> selected = mealPlans.getOrDefault(dateStr, new HashMap<>())
                .getOrDefault(mealType, new ArrayList<>());

        for (Page4.Recipe r : allRecipes) {
            recipePanel.add(createRecipeCard(r, () -> {
                if (selected.contains(r)) selected.remove(r);
                else {
                    selected.add(r);
                    JOptionPane.showMessageDialog(dialog, r.name + " added to " + mealType + " successfully!");
                }
                saveMealPlan(dateStr, mealType, selected);
                refreshCalendar();
            }, selected.contains(r) ? "Remove" : "Add"));
        }

        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Save & Back");
        closeButton.addActionListener(e -> {
            saveMealPlan(dateStr, mealType, selected);
            dialog.dispose();
            openDaySidebar(dateStr, dayButton);
        });
        dialog.add(closeButton, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    //==================================================================================================================
    // Recipe Card
    //==================================================================================================================
    private JPanel createRecipeCard(Page4.Recipe recipe, Runnable onClick, String buttonLabel) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(Color.GRAY, 1)));
        card.setMaximumSize(new Dimension(600, 120));

        JLabel imgLabel = new JLabel(Page4.loadImageIconStatic(recipe.imagePath, 100, 100));
        card.add(imgLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(5, 10, 5, 5));
        infoPanel.add(new JLabel(recipe.name));
        infoPanel.add(new JLabel("Calories: " + recipe.calories));
        infoPanel.add(new JLabel("Protein: " + recipe.protein + "g"));
        infoPanel.add(new JLabel("Carbs: " + recipe.carbs + "g"));
        infoPanel.add(new JLabel("Cost: $" + recipe.cost));
        card.add(infoPanel, BorderLayout.CENTER);

        JButton button = new JButton(buttonLabel);
        button.addActionListener(e -> onClick.run());
        card.add(button, BorderLayout.EAST);

        return card;
    }

    //==================================================================================================================
    // File Handling
    //==================================================================================================================
    private void loadAllMealPlans() {
        mealPlans.clear();
        File file = new File(MEAL_PLAN_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String date = parts[0];
                String mealType = parts[1];
                String recipeName = parts[2];

                Page4.Recipe recipe = findRecipeByName(recipeName);
                if (recipe == null) continue;

                mealPlans.computeIfAbsent(date, k -> new HashMap<>())
                        .computeIfAbsent(mealType, k -> new ArrayList<>())
                        .add(recipe);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMealPlan(String dateStr, String mealType, List<Page4.Recipe> selectedRecipes) {
        mealPlans.computeIfAbsent(dateStr, k -> new HashMap<>()).put(mealType, selectedRecipes);

        try (PrintWriter pw = new PrintWriter(new FileWriter(MEAL_PLAN_FILE))) {
            for (String date : mealPlans.keySet()) {
                for (String meal : mealPlans.get(date).keySet()) {
                    for (Page4.Recipe r : mealPlans.get(date).get(meal)) {
                        pw.println(date + "|" + meal + "|" + r.name);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Page4.Recipe findRecipeByName(String name) {
        List<Page4.Recipe> all = new ArrayList<>();
        all.addAll(Page4.loadRecipesFromFile("src/pages/text/recipes.txt"));
        all.addAll(Page4.loadRecipesFromFile("src/pages/text/custom_recipes.txt"));
        for (Page4.Recipe r : all) {
            if (r.name.equalsIgnoreCase(name)) return r;
        }
        return null;
    }
}
