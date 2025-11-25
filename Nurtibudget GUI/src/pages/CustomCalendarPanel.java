package pages;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Locale;

import org.json.*;

/*
Overview:
- Calendar UI: month + weekly views, integrates with inventory batches and meal plans.
- Batch labels use batch date (MM/dd) and an index suffix if multiple batches share the same date/recipe.
- Weekly waste calculations only consider batches whose dateMade falls inside the viewed week, and count
  assignments for each batch only within that same week.
- Adds "Refresh week" button that validates and repairs the current week: remaps missing inventory IDs,
  creates new batches when needed, persists inventory and meal plans, and refreshes the weekly view.
*/

//======================================================================================================================
// Imports & Top-level DTOs
//======================================================================================================================

public class CustomCalendarPanel extends JPanel {

    //==============================================================================================================
    // Fields & Constants
    //==============================================================================================================
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_BATCH_DATE = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter HEADER_WEEK_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());
    private static final String MEAL_PLAN_FILE = "src/pages/text/meal_plans.txt";
    private static final String INVENTORY_FILE = "src/pages/text/inventory.json";

    private static final Color COLOR_TODAY = new Color(173, 216, 230);
    private static final Color COLOR_HAS_MEALS = new Color(144, 238, 144);
    private static final Color COLOR_EXPIRE_BADGE = new Color(255, 204, 102);

    private JLabel monthYearLabel;
    private JPanel calendarPanel;
    private LocalDate currentDate;
    private Map<String, Map<String, List<PlannedMeal>>> mealPlans = new HashMap<>();
    private boolean isWeeklyView = false;
    private LocalDate currentWeekStart;
    private Map<String, String> batchLabelMap = new HashMap<>();
    private boolean showBatchLabels = false;
    private JButton navPrevButton;
    private JButton navNextButton;
    private JButton viewToggleButton;

    //==============================================================================================================
    // Simple DTOs
    //==============================================================================================================
    private static class PlannedMeal {
        Page4.Recipe recipe;
        String inventoryId;

        PlannedMeal(Page4.Recipe recipe, String inventoryId) {
            this.recipe = recipe;
            this.inventoryId = inventoryId;
        }
    }

    private static class InventoryEntry {
        String id;
        String name;
        LocalDate dateMade;
        int edibleDays;
        int totalPortions;
        int portionsUsed;
        double pricePerPortion;
        String notes;

        static InventoryEntry fromJson(JSONObject o) {
            InventoryEntry e = new InventoryEntry();
            e.id = o.optString("id", UUID.randomUUID().toString());
            e.name = o.optString("name", "");
            String ds = o.optString("dateMade", o.optString("purchaseDate", LocalDate.now().format(DATE_FORMAT)));
            e.dateMade = LocalDate.parse(ds, DATE_FORMAT);
            e.edibleDays = o.optInt("edibleDays", 3);
            e.totalPortions = o.optInt("totalPortions", 1);
            e.portionsUsed = o.optInt("portionsUsed", 0);
            e.pricePerPortion = o.optDouble("pricePerPortion", 0.0);
            e.notes = o.optString("notes", "");
            return e;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("dateMade", dateMade.format(DATE_FORMAT));
            o.put("edibleDays", edibleDays);
            o.put("totalPortions", totalPortions);
            o.put("portionsUsed", portionsUsed);
            o.put("pricePerPortion", pricePerPortion);
            o.put("notes", notes != null ? notes : "");
            return o;
        }
    }

    //==============================================================================================================
    // Constructor & Initialization
    //==============================================================================================================
    public CustomCalendarPanel() {
        setLayout(new BorderLayout());
        currentDate = LocalDate.now();
        currentWeekStart = currentDate.with(DayOfWeek.SUNDAY);

        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navPrevButton = new JButton("<");
        navNextButton = new JButton(">");
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        viewToggleButton = new JButton("Weekly View");
        JButton inventoryButton = new JButton("Inventory");
        JCheckBox showBatchesToggle = new JCheckBox("Show batch labels");
        showBatchesToggle.setSelected(showBatchLabels);
        showBatchesToggle.addActionListener(e -> {
            showBatchLabels = showBatchesToggle.isSelected();
            buildBatchLabels();
            refreshCalendar();
        });

        navPrevButton.addActionListener(e -> {
            if (isWeeklyView) {
                currentWeekStart = currentWeekStart.minusWeeks(1);
                openWeeklyPanel(currentWeekStart);
            } else {
                currentDate = currentDate.minusMonths(1);
                refreshCalendar();
            }
        });
        navNextButton.addActionListener(e -> {
            if (isWeeklyView) {
                currentWeekStart = currentWeekStart.plusWeeks(1);
                openWeeklyPanel(currentWeekStart);
            } else {
                currentDate = currentDate.plusMonths(1);
                refreshCalendar();
            }
        });

        viewToggleButton.addActionListener(e -> {
            if (isWeeklyView) {
                isWeeklyView = false;
                viewToggleButton.setText("Weekly View");
                refreshCalendar();
            } else {
                isWeeklyView = true;
                viewToggleButton.setText("Month View");
                openWeeklyPanel(currentWeekStart);
            }
        });

        inventoryButton.addActionListener(e -> {
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            InventoryDialog dlg = new InventoryDialog(frame, () -> {
                buildBatchLabels();
                loadAllMealPlans();
                if (isWeeklyView) openWeeklyPanel(currentWeekStart);
                else refreshCalendar();
            });
            dlg.setVisible(true);
            buildBatchLabels();
            loadAllMealPlans();
            refreshCalendar();
        });

        navPanel.add(navPrevButton);
        navPanel.add(monthYearLabel);
        navPanel.add(navNextButton);
        navPanel.add(Box.createHorizontalStrut(8));
        navPanel.add(viewToggleButton);
        navPanel.add(inventoryButton);
        navPanel.add(showBatchesToggle);

        JPanel legendPanel = buildLegendPanel();
        headerPanel.add(navPanel, BorderLayout.CENTER);
        headerPanel.add(legendPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        calendarPanel = new JPanel(new BorderLayout());
        add(calendarPanel, BorderLayout.CENTER);

        buildBatchLabels();
        loadAllMealPlans();
        refreshCalendar();
    }

    //==============================================================================================================
    // Small UI Helpers
    //==============================================================================================================
    private JPanel buildLegendPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        p.setOpaque(false);
        p.add(labelForColor(COLOR_TODAY, "Today"));
        p.add(labelForColor(COLOR_HAS_MEALS, "Has meals"));
        p.add(labelForColor(COLOR_EXPIRE_BADGE, "Expiring soon (badge)"));
        JButton info = new JButton("How it works");
        info.addActionListener(e -> showHowItWorksDialog());
        p.add(info);
        return p;
    }

    private JComponent labelForColor(Color c, String text) {
        JPanel box = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        box.setOpaque(false);
        JLabel colorDot = new JLabel();
        colorDot.setOpaque(true);
        colorDot.setBackground(c);
        colorDot.setPreferredSize(new Dimension(14, 14));
        colorDot.setBorder(new LineBorder(Color.DARK_GRAY, 1));
        JLabel lbl = new JLabel(" " + text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        box.add(colorDot);
        box.add(lbl);
        return box;
    }

    private void showHowItWorksDialog() {
        String text = "<html>"
                + "<h2>How the calendar works (simple)</h2>"
                + "<p>- Inventory holds batches of cooked food. Each batch has how many portions, a date made, and how many days it stays good.</p>"
                + "<p>- To add a meal you pick a batch. If a batch is old or empty it won't be shown.</p>"
                + "<p>- The calendar shows meals you planned. If a meal is close to its expiry you'll see a yellow 'Expiring' badge on that meal's detail page.</p>"
                + "<p>- Weekly view shows how many portions may be left at the end of the week and the dollar value of that leftover food.</p>"
                + "</html>";
        JEditorPane ep = new JEditorPane("text/html", text);
        ep.setEditable(false);
        ep.setOpaque(false);
        ep.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        ep.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(ep);
        sp.setPreferredSize(new Dimension(520, 300));
        JOptionPane.showMessageDialog(this, sp, "How it works", JOptionPane.INFORMATION_MESSAGE);
    }

    //==============================================================================================================
    // Month Rendering
    //==============================================================================================================
    private void refreshCalendar() {
        isWeeklyView = false;
        viewToggleButton.setText("Weekly View");
        calendarPanel.removeAll();

        JPanel daysHeader = new JPanel(new GridLayout(1, 7, 5, 5));
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : dayNames) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            dayLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
            daysHeader.add(dayLabel);
        }

        JPanel daysGrid = new JPanel(new GridLayout(0, 7, 5, 5));
        daysGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        YearMonth yearMonth = YearMonth.from(currentDate);
        monthYearLabel.setText(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + yearMonth.getYear());

        LocalDate firstDay = yearMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = yearMonth.lengthOfMonth();

        for (int i = 0; i < startDay; i++) daysGrid.add(new JLabel(""));

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFocusPainted(false);
            dayButton.setOpaque(true);

            String dateStr = DATE_FORMAT.format(date);
            boolean hasMealPlan = mealPlans.containsKey(dateStr)
                    && mealPlans.get(dateStr).values().stream().anyMatch(l -> !l.isEmpty());

            StringBuilder tooltip = new StringBuilder("<html><b>").append(dateStr).append("</b><br/>");

            if (hasMealPlan) {
                Map<String, List<PlannedMeal>> dayMap = mealPlans.get(dateStr);
                int totalMeals = 0;
                for (Map.Entry<String, List<PlannedMeal>> entry : dayMap.entrySet()) {
                    String mealType = entry.getKey();
                    List<PlannedMeal> list = entry.getValue();
                    if (list.isEmpty()) continue;
                    tooltip.append("<u>").append(mealType).append("</u><br/>");
                    for (PlannedMeal pm : list) {
                        totalMeals++;
                        Page4.Recipe r = pm.recipe;

                        LocalDate batchDate = null;
                        int edibleDays = getEdibleDaysOrDefault(r);
                        if (pm.inventoryId != null) {
                            InventoryEntry ie = findInventoryById(pm.inventoryId);
                            if (ie != null) {
                                batchDate = ie.dateMade;
                                edibleDays = ie.edibleDays;
                            }
                        }
                        LocalDate cooked = batchDate != null ? batchDate : findEarliestAssignmentDateForRecipe(r.recipe_name).orElse(date);
                        LocalDate expiry = cooked.plusDays(Math.max(0, edibleDays));

                        int totalPortions = getTotalPortionsOrDefault(r);
                        int assignedUpToDate = countAssignmentsOfRecipeUpToDate(r.recipe_name, date);
                        int assignedUpToWeekEnd = countAssignmentsOfRecipeUpToDate(r.recipe_name, date.with(DayOfWeek.SUNDAY).plusDays(6));
                        int remainingNow = Math.max(0, totalPortions - assignedUpToDate);
                        int remainingWeekEnd = Math.max(0, totalPortions - assignedUpToWeekEnd);

                        double pricePerPortion = getPriceForPlannedMeal(pm);
                        double moneyWastedWeek = remainingWeekEnd * pricePerPortion;
                        long daysUntilExpiry = ChronoUnit.DAYS.between(date, expiry);

                        String batchLabel = batchLabelOrShort(pm.inventoryId);
                        tooltip.append(String.format("%s %s — now:%d, wkend:%d, expires in:%d d, $wk: $%.2f<br/>",
                                escapeHtml(r.recipe_name),
                                (showBatchLabels && batchLabel != null ? "(" + batchLabel + ")" : ""),
                                remainingNow, remainingWeekEnd, Math.max(0, (int) daysUntilExpiry), moneyWastedWeek));
                    }
                }
                tooltip.append(String.format("<br/>Total planned items: %d<br/>", totalMeals));
            } else {
                tooltip.append("No meals planned.<br/>");
            }
            tooltip.append("</html>");
            dayButton.setToolTipText(tooltip.toString());

            if (date.equals(LocalDate.now())) {
                dayButton.setBackground(COLOR_TODAY);
            } else if (hasMealPlan) {
                dayButton.setBackground(COLOR_HAS_MEALS);
            } else {
                dayButton.setBackground(Color.WHITE);
            }

            dayButton.addActionListener(e -> openDaySidebar(dateStr, dayButton));
            daysGrid.add(dayButton);
        }

        calendarPanel.add(daysHeader, BorderLayout.NORTH);
        calendarPanel.add(daysGrid, BorderLayout.CENTER);
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    //==============================================================================================================
    // Day Sidebar
    //==============================================================================================================
    private void openDaySidebar(String dateStr, JButton dayButton) {
        Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(dateStr, new HashMap<>());
        JDialog dialog = new JDialog((Frame) null, "Planned Recipes - " + dateStr, true);
        dialog.setSize(520, 700);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);

        for (String mealType : new String[]{"Breakfast", "Lunch", "Dinner", "Snack"}) {
            List<PlannedMeal> recipes = dayMeals.getOrDefault(mealType, new ArrayList<>());

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

            for (PlannedMeal pm : recipes) {
                Page4.Recipe r = pm.recipe;
                JPanel card = new JPanel(new BorderLayout(12, 8));
                card.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(Color.GRAY, 1)));
                card.setMaximumSize(new Dimension(480, 120));

                JLabel imgLabel = new JLabel(loadImageIconStatic(r.imagePath, 80, 80));
                card.add(imgLabel, BorderLayout.WEST);

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setBorder(new EmptyBorder(5, 10, 5, 5));

                JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                titleRow.setOpaque(false);
                JLabel nameLbl = new JLabel(r.recipe_name);
                nameLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                titleRow.add(nameLbl);

                LocalDate expiry = getExpiryDateForPlanned(pm);
                if (expiry != null) {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(date, expiry);
                    if (daysUntilExpiry >= 0 && daysUntilExpiry <= 1) {
                        JLabel badge = new JLabel("Expiring");
                        badge.setOpaque(true);
                        badge.setBackground(COLOR_EXPIRE_BADGE);
                        badge.setBorder(new EmptyBorder(3, 6, 3, 6));
                        titleRow.add(badge);
                    }
                }

                infoPanel.add(titleRow);

                double price = getPriceForPlannedMeal(pm);

                String batchLabel = batchLabelOrShort(pm.inventoryId);
                String invText = (showBatchLabels && batchLabel != null) ? ("Batch: " + batchLabel) : "";
                infoPanel.add(new JLabel(String.format("Price: $%.2f %s | Nutrition: %s",
                        price, invText, r.nutrition_grade)));

                card.add(infoPanel, BorderLayout.CENTER);

                JButton removeBtn = new JButton("Remove");
                removeBtn.addActionListener(e -> {
                    List<PlannedMeal> recipesList = dayMeals.getOrDefault(mealType, new ArrayList<>());
                    recipesList.remove(pm);
                    saveMealPlan(dateStr, mealType, recipesList);
                    contentPanel.remove(card);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                    refreshCalendar();
                });
                card.add(removeBtn, BorderLayout.EAST);

                contentPanel.add(card);
                contentPanel.add(Box.createVerticalStrut(6));
            }

            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            contentPanel.add(separator);
            contentPanel.add(Box.createVerticalStrut(6));
        }

        JButton addMealBtn = new JButton("Add Meal (inventory required)");
        addMealBtn.addActionListener(e -> {
            dialog.dispose();
            openMealEditorDialog(dateStr, null, dayButton);
        });

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(addMealBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //==============================================================================================================
    // Meal Editor (inventory picker)
    //==============================================================================================================
    private void openMealEditorDialog(String dateStr, String mealType, JButton dayButton) {
        JDialog dialog = new JDialog((Frame) null, "Add from Inventory - " + dateStr, true);
        dialog.setSize(820, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        LocalDate targetDate = LocalDate.parse(dateStr, DATE_FORMAT);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(content);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        List<InventoryEntry> candidates = new ArrayList<>();
        for (InventoryEntry ie : loadInventoryEntries()) {
            if (ie.dateMade.isAfter(targetDate)) continue;
            LocalDate expiry = ie.dateMade.plusDays(Math.max(0, ie.edibleDays));
            if (expiry.isBefore(targetDate)) continue;
            int assigned = countAssignmentsOfInventory(ie.id);
            int remaining = ie.totalPortions - ie.portionsUsed - assigned;
            if (remaining <= 0) continue;
            candidates.add(ie);
        }
        candidates.sort(Comparator.comparing((InventoryEntry a) -> a.name.toLowerCase()).thenComparing(a -> a.dateMade));

        if (candidates.isEmpty()) {
            JLabel none = new JLabel("<html><i>No inventory batches available for this date. Add batches in Inventory to make them available.</i></html>");
            none.setBorder(new EmptyBorder(10, 10, 10, 10));
            content.add(none);
        } else {
            for (InventoryEntry ie : candidates) {
                Page4.Recipe r = findRecipeByName(ie.name);
                JPanel row = new JPanel(new BorderLayout(12, 6));
                row.setBorder(new CompoundBorder(new EmptyBorder(6,6,6,6), new LineBorder(Color.GRAY,1)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

                JLabel img = new JLabel();
                ImageIcon ic = null;
                if (r != null) ic = loadImageIconStatic(r.imagePath, 100, 80);
                if (ic != null) img.setIcon(ic);
                else {
                    img.setText("[No Image]");
                    img.setForeground(Color.GRAY);
                    img.setPreferredSize(new Dimension(100, 80));
                }
                row.add(img, BorderLayout.WEST);

                JPanel mid = new JPanel();
                mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
                String batchLabel = batchLabelOrShort(ie.id);
                String displayLabel = (showBatchLabels && batchLabel != null) ? (batchLabel + " • ") : "";
                JLabel title = new JLabel("<html><b>" + displayLabel + escapeHtml(ie.name) + "</b></html>");
                mid.add(title);
                JLabel meta = new JLabel(String.format("Made: %s | Expires: %s | Remaining: %d | $/serv: $%.2f",
                        ie.dateMade.format(DATE_FORMAT),
                        ie.dateMade.plusDays(Math.max(0, ie.edibleDays)).format(DATE_FORMAT),
                        ie.totalPortions - ie.portionsUsed - countAssignmentsOfInventory(ie.id),
                        ie.pricePerPortion));
                mid.add(meta);
                if (ie.notes != null && !ie.notes.isEmpty()) mid.add(new JLabel("<html><i>" + escapeHtml(ie.notes) + "</i></html>"));
                row.add(mid, BorderLayout.CENTER);

                JPanel right = new JPanel(new GridLayout(2,1,4,4));
                JButton addBtn = new JButton("Add");
                addBtn.addActionListener(e -> {
                    String chosenMealType = mealType;
                    if (chosenMealType == null) {
                        chosenMealType = (String) JOptionPane.showInputDialog(this, "Select meal type to add to:",
                                "Select Meal Type", JOptionPane.PLAIN_MESSAGE, null, new String[]{"Breakfast","Lunch","Dinner","Snack"}, "Dinner");
                        if (chosenMealType == null) return;
                    }
                    Page4.Recipe recipe = r != null ? r : createPlaceholderRecipe(ie.name);
                    PlannedMeal pm = new PlannedMeal(recipe, ie.id);
                    List<PlannedMeal> selected = mealPlans.getOrDefault(dateStr, new HashMap<>())
                            .getOrDefault(chosenMealType, new ArrayList<>());
                    selected.add(pm);
                    saveMealPlan(dateStr, chosenMealType, selected);
                    dialog.dispose();
                    openDaySidebar(dateStr, dayButton);
                    refreshCalendar();
                });
                right.add(addBtn);

                JButton details = new JButton("Details");
                details.addActionListener(e -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Batch: ").append(batchLabelOrShort(ie.id)).append("\n");
                    sb.append("Recipe: ").append(ie.name).append("\n");
                    sb.append("Made: ").append(ie.dateMade.format(DATE_FORMAT)).append("\n");
                    sb.append("Expires: ").append(ie.dateMade.plusDays(Math.max(0, ie.edibleDays)).format(DATE_FORMAT)).append("\n");
                    sb.append("Remaining: ").append(ie.totalPortions - ie.portionsUsed - countAssignmentsOfInventory(ie.id)).append("\n");
                    sb.append("Price / portion: $").append(String.format("%.2f", ie.pricePerPortion)).append("\n");
                    sb.append("Notes: ").append(ie.notes == null ? "" : ie.notes).append("\n");
                    JOptionPane.showMessageDialog(this, sb.toString(), "Batch details", JOptionPane.INFORMATION_MESSAGE);
                });
                right.add(details);

                row.add(right, BorderLayout.EAST);

                content.add(row);
                content.add(Box.createVerticalStrut(6));
            }
        }

        dialog.add(sp, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        dialog.add(close, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    //==============================================================================================================
    // Weekly View & "Refresh week" button integration
    //==============================================================================================================
    private void openWeeklyPanel(LocalDate weekStart) {
        isWeeklyView = true;
        currentWeekStart = weekStart.with(DayOfWeek.SUNDAY);
        buildBatchLabels();
        loadAllMealPlans();
        viewToggleButton.setText("Month View");
        calendarPanel.removeAll();

        JPanel main = new JPanel(new BorderLayout());

        JPanel weeklyHeader = new JPanel(new BorderLayout());
        JPanel weekNav = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton prevWeek = new JButton("< Week");
        JButton nextWeek = new JButton("Week >");
        JLabel weekLabel = new JLabel("Week of " + currentWeekStart.format(HEADER_WEEK_FORMAT));
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton copyWeekBtn = new JButton("Copy Week");

        // Refresh-week button
        JButton refreshWeekBtn = new JButton("Refresh Week");
        refreshWeekBtn.addActionListener(e -> {
            performWeekValidationAndRepair(currentWeekStart);
        });

        prevWeek.addActionListener(e -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            openWeeklyPanel(currentWeekStart);
        });
        nextWeek.addActionListener(e -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            openWeeklyPanel(currentWeekStart);
        });
        copyWeekBtn.addActionListener(e -> openCopyWeekDialog());

        weekNav.add(prevWeek);
        weekNav.add(weekLabel);
        weekNav.add(nextWeek);
        weekNav.add(Box.createHorizontalStrut(8));
        weekNav.add(copyWeekBtn);
        weekNav.add(Box.createHorizontalStrut(6));
        weekNav.add(refreshWeekBtn); // add it to the header
        weeklyHeader.add(weekNav, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JCheckBox topBatchToggle = new JCheckBox("Show batch labels");
        topBatchToggle.setSelected(showBatchLabels);
        topBatchToggle.addActionListener(e -> {
            showBatchLabels = topBatchToggle.isSelected();
            buildBatchLabels();
            openWeeklyPanel(currentWeekStart);
        });
        right.add(topBatchToggle);
        weeklyHeader.add(right, BorderLayout.EAST);

        main.add(weeklyHeader, BorderLayout.NORTH);

        // week grid & sidebar (unchanged layout)...
        JPanel weekGrid = new JPanel(new GridLayout(1, 7, 6, 6));
        weekGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < 7; i++) {
            LocalDate d = currentWeekStart.plusDays(i);
            String dateStr = DATE_FORMAT.format(d);

            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
            dayPanel.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1), new EmptyBorder(8, 8, 8, 8)));
            dayPanel.setPreferredSize(new Dimension(200, 300));

            JLabel dayLabel = new JLabel(d.getDayOfWeek().toString() + " " + d.format(DATE_FORMAT));
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            dayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            dayPanel.add(dayLabel);
            dayPanel.add(Box.createVerticalStrut(6));

            Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(dateStr, new HashMap<>());
            boolean any = false;
            for (String mealType : new String[]{"Breakfast", "Lunch", "Dinner", "Snack"}) {
                List<PlannedMeal> list = dayMeals.getOrDefault(mealType, Collections.emptyList());
                if (!list.isEmpty()) {
                    any = true;
                    JLabel mt = new JLabel(mealType + " (" + list.size() + ")");
                    mt.setFont(new Font("SansSerif", Font.BOLD, 11));
                    mt.setAlignmentX(Component.LEFT_ALIGNMENT);
                    dayPanel.add(mt);

                    for (PlannedMeal pm : list) {
                        Page4.Recipe r = pm.recipe;
                        String batchLabel = batchLabelOrShort(pm.inventoryId);
                        double price = getPriceForPlannedMeal(pm);

                        String infoHtml = String.format("<html><div style='width:170px'>%s <br/><i>%s</i> - $%.2f/serv</div></html>",
                                escapeHtml(r.recipe_name),
                                (showBatchLabels && batchLabel != null ? batchLabel : ""),
                                price);
                        JLabel l = new JLabel(infoHtml);
                        l.setAlignmentX(Component.LEFT_ALIGNMENT);
                        dayPanel.add(l);
                    }
                    dayPanel.add(Box.createVerticalStrut(6));
                }
            }

            if (!any) {
                JLabel none = new JLabel("No meals planned");
                none.setForeground(Color.GRAY);
                none.setAlignmentX(Component.LEFT_ALIGNMENT);
                dayPanel.add(none);
            }

            weekGrid.add(dayPanel);
        }

        JScrollPane weekScroll = new JScrollPane(weekGrid, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        weekScroll.setBorder(null);
        main.add(weekScroll, BorderLayout.CENTER);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1), new EmptyBorder(8, 8, 8, 8)));
        sidebar.setPreferredSize(new Dimension(360, 0));

        JLabel summaryTitle = new JLabel("Weekly Summary & Waste");
        summaryTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        sidebar.add(summaryTitle);
        sidebar.add(Box.createVerticalStrut(8));

        LocalDate weekEnd = currentWeekStart.plusDays(6);

        double totalPotentialWasteDollars = 0.0;
        int totalPotentialWastePortions = 0;

        List<InventoryEntry> invList = loadInventoryEntries();
        for (InventoryEntry ie : invList) {
            if (ie.dateMade.isBefore(currentWeekStart) || ie.dateMade.isAfter(weekEnd)) continue;
            int assignedInWeek = countAssignmentsOfInventoryInRange(ie.id, currentWeekStart, weekEnd);
            int remainingAtWeekEnd = Math.max(0, ie.totalPortions - ie.portionsUsed - assignedInWeek);
            double moneyWasted = remainingAtWeekEnd * ie.pricePerPortion;
            totalPotentialWasteDollars += moneyWasted;
            totalPotentialWastePortions += remainingAtWeekEnd;
        }

        sidebar.add(makeBigStatLabel("Leftover portions (wk end)", String.valueOf(totalPotentialWastePortions)));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(makeBigStatLabel("Potential money lost", String.format("$%.2f", totalPotentialWasteDollars)));
        sidebar.add(Box.createVerticalStrut(12));

        JButton breakdownBtn = new JButton("View waste breakdown");
        breakdownBtn.addActionListener(e -> showWasteBreakdown(currentWeekStart, weekEnd));
        sidebar.add(breakdownBtn);
        sidebar.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("<html><i>Only non-expired batches with remaining portions are offered when adding meals.</i></html>");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        sidebar.add(hint);
        sidebar.add(Box.createVerticalStrut(8));

        JButton backBtn = new JButton("Back to Month");
        backBtn.addActionListener(e -> {
            isWeeklyView = false;
            viewToggleButton.setText("Weekly View");
            refreshCalendar();
        });
        sidebar.add(backBtn);

        JScrollPane sidebarScroll = new JScrollPane(sidebar);
        sidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setPreferredSize(new Dimension(360, 0));

        main.add(sidebarScroll, BorderLayout.EAST);

        calendarPanel.add(main, BorderLayout.CENTER);
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    //==============================================================================================================
    // Week validation & repair
    //==============================================================================================================
    private void performWeekValidationAndRepair(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        try {
            JSONArray invArr = readInventoryArray();
            // create a mutable list of InventoryEntry from invArr for in-memory search
            List<InventoryEntry> currentInv = new ArrayList<>();
            for (int i = 0; i < invArr.length(); i++) currentInv.add(InventoryEntry.fromJson(invArr.getJSONObject(i)));

            int remapped = 0;
            int created = 0;

            for (int i = 0; i < 7; i++) {
                LocalDate d = weekStart.plusDays(i);
                String dKey = d.format(DATE_FORMAT);
                Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(dKey, Collections.emptyMap());
                for (Map.Entry<String, List<PlannedMeal>> entry : dayMeals.entrySet()) {
                    List<PlannedMeal> list = entry.getValue();
                    for (PlannedMeal pm : list) {
                        // if inventoryId exists and points to an entry on disk, OK
                        if (pm.inventoryId != null && findInventoryById(pm.inventoryId) != null) continue;
                        // otherwise attempt to find a matching batch in currentInv for same recipe and date
                        String recipeName = pm.recipe != null ? pm.recipe.recipe_name : null;
                        InventoryEntry candidate = findCandidateBatchForDate(currentInv, recipeName, d);
                        if (candidate != null) {
                            pm.inventoryId = candidate.id;
                            remapped++;
                            continue;
                        }
                        // not found -> create a new batch for this recipe on this date and append
                        InventoryEntry newBatch = createBatchForPlannedMeal(pm, d);
                        invArr.put(newBatch.toJson());
                        currentInv.add(newBatch);
                        pm.inventoryId = newBatch.id;
                        created++;
                    }
                }
            }

            // persist inventory (if any new entries were appended)
            writeInventoryArray(invArr);

            // persist meal_plans
            try (PrintWriter pw = new PrintWriter(new FileWriter(MEAL_PLAN_FILE))) {
                for (String date : mealPlans.keySet()) {
                    for (String meal : mealPlans.get(date).keySet()) {
                        for (PlannedMeal pm : mealPlans.get(date).get(meal)) {
                            String inv = pm.inventoryId != null ? pm.inventoryId : "";
                            pw.println(date + "|" + meal + "|" + pm.recipe.recipe_name + "|" + inv);
                        }
                    }
                }
            }

            // refresh UI
            buildBatchLabels();
            loadAllMealPlans();
            openWeeklyPanel(weekStart);

            String msg = String.format("Week validation complete. Remapped: %d assignments. Created new batches: %d.", remapped, created);
            JOptionPane.showMessageDialog(this, msg, "Refresh Week", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Week refresh failed: " + ex.getMessage(), "Refresh Week", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // find candidate batch: same recipe name, same date (exact) and with remaining portions; if none, allow any in-week
    private InventoryEntry findCandidateBatchForDate(List<InventoryEntry> invList, String recipeName, LocalDate date) {
        if (recipeName == null) return null;
        // exact date first
        for (InventoryEntry ie : invList) {
            if (ie.name.equalsIgnoreCase(recipeName) && ie.dateMade.equals(date)) {
                int assigned = countAssignmentsOfInventoryInRange(ie.id, date, date);
                int remaining = ie.totalPortions - ie.portionsUsed - assigned;
                if (remaining > 0) return ie;
            }
        }
        // allow same-week other days
        LocalDate weekStart = date.with(DayOfWeek.SUNDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        for (InventoryEntry ie : invList) {
            if (ie.name.equalsIgnoreCase(recipeName) && !ie.dateMade.isBefore(weekStart) && !ie.dateMade.isAfter(weekEnd)) {
                int assigned = countAssignmentsOfInventoryInRange(ie.id, weekStart, weekEnd);
                int remaining = ie.totalPortions - ie.portionsUsed - assigned;
                if (remaining > 0) return ie;
            }
        }
        return null;
    }

    // create a new inventory batch for a planned meal on targetDate
    private InventoryEntry createBatchForPlannedMeal(PlannedMeal pm, LocalDate targetDate) {
        InventoryEntry newIe = new InventoryEntry();
        newIe.id = UUID.randomUUID().toString();
        newIe.name = pm.recipe != null ? pm.recipe.recipe_name : "Unknown";
        newIe.dateMade = targetDate;
        newIe.edibleDays = getEdibleDaysOrDefault(pm.recipe);
        newIe.totalPortions = getTotalPortionsOrDefault(pm.recipe);
        newIe.portionsUsed = 0;
        newIe.pricePerPortion = pm.recipe != null ? pm.recipe.cost_per_serving : 0.0;
        newIe.notes = "Auto-created by week refresh";
        return newIe;
    }

    //==============================================================================================================
    // Pricing & Waste helpers
    //==============================================================================================================
    private double getPriceForPlannedMeal(PlannedMeal pm) {
        if (pm == null || pm.recipe == null) return 0.0;
        if (pm.inventoryId != null) {
            InventoryEntry ie = findInventoryById(pm.inventoryId);
            if (ie != null) return ie.pricePerPortion;
        }
        try {
            return pm.recipe.cost_per_serving;
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private void showWasteBreakdown(LocalDate weekStart, LocalDate weekEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Waste breakdown for week %s - %s%n", weekStart.format(DATE_FORMAT), weekEnd.format(DATE_FORMAT)));
        sb.append(String.format("%-12s | %-20s | %-10s | %-10s | %-6s | %-6s | %-8s | %-8s%n",
                "BatchLabel", "Recipe", "Made", "Expires", "$/serv", "Total", "Assigned", "Remaining"));
        sb.append(String.join("", Collections.nCopies(120, "-"))).append("\n");

        List<InventoryEntry> invList2 = loadInventoryEntries();
        invList2.sort(Comparator.comparing((InventoryEntry i) -> i.name.toLowerCase()).thenComparing(i -> i.dateMade));

        for (InventoryEntry ie : invList2) {
            if (ie.dateMade.isBefore(weekStart) || ie.dateMade.isAfter(weekEnd)) continue;
            int assignedInWeek = countAssignmentsOfInventoryInRange(ie.id, weekStart, weekEnd);
            int remainingAtWeekEnd = Math.max(0, ie.totalPortions - ie.portionsUsed - assignedInWeek);
            LocalDate expiry = ie.dateMade.plusDays(Math.max(0, ie.edibleDays));
            String label = batchLabelOrShort(ie.id);
            String recipe = ie.name.length() > 20 ? ie.name.substring(0, 20) : ie.name;
            sb.append(String.format("%-12s | %-20s | %-10s | %-10s | %6.2f | %-6d | %-8d | %-8d%n",
                    (label != null ? label : ""),
                    recipe,
                    ie.dateMade.format(DATE_FORMAT),
                    expiry.format(DATE_FORMAT),
                    ie.pricePerPortion,
                    ie.totalPortions,
                    assignedInWeek,
                    remainingAtWeekEnd));
        }

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(820, 420));
        JOptionPane.showMessageDialog(this, sp, "Waste breakdown", JOptionPane.INFORMATION_MESSAGE);
    }

    //==============================================================================================================
    // Copy week (keeps new batches independent)
    //==============================================================================================================
    private void openCopyWeekDialog() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8,8,8,8));

        List<LocalDate> sundays = new ArrayList<>();
        LocalDate center = currentWeekStart.with(DayOfWeek.SUNDAY);
        int range = 26;
        for (int i = -range; i <= range; i++) sundays.add(center.plusWeeks(i));

        Vector<String> sundayLabels = new Vector<>();
        for (LocalDate d : sundays) sundayLabels.add(d.format(DATE_FORMAT) + " (Week of " + d.format(HEADER_WEEK_FORMAT) + ")");

        JComboBox<String> srcCombo = new JComboBox<>(sundayLabels);
        JComboBox<String> dstCombo = new JComboBox<>(sundayLabels);
        int centerIndex = range;
        srcCombo.setSelectedIndex(centerIndex);
        dstCombo.setSelectedIndex(centerIndex + 1 <= 2*range ? centerIndex + 1 : centerIndex);

        p.add(new JLabel("Source week (select the week's Sunday):"));
        p.add(srcCombo);
        p.add(Box.createVerticalStrut(8));
        p.add(new JLabel("Destination week (select the week's Sunday):"));
        p.add(dstCombo);
        p.add(Box.createVerticalStrut(12));

        String[] options = {"Merge (append)", "Overwrite (replace)"};
        JComboBox<String> modeBox = new JComboBox<>(options);
        p.add(new JLabel("Mode:"));
        p.add(modeBox);

        int res = JOptionPane.showConfirmDialog(this, p, "Copy Week", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            int srcIndex = srcCombo.getSelectedIndex();
            int dstIndex = dstCombo.getSelectedIndex();
            LocalDate srcSunday = sundays.get(srcIndex);
            LocalDate dstSunday = sundays.get(dstIndex);

            long deltaDays = ChronoUnit.DAYS.between(srcSunday, dstSunday);
            String mode = (String) modeBox.getSelectedItem();

            Map<Integer, Map<String, List<PlannedMeal>>> weekData = new HashMap<>();
            Map<String, List<PlannedMealRef>> origBatchUsage = new HashMap<>();
            Map<String, List<Integer>> recipeOnlyUsage = new HashMap<>();

            for (int i = 0; i < 7; i++) {
                LocalDate sDay = srcSunday.plusDays(i);
                String sKey = sDay.format(DATE_FORMAT);
                Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(sKey, Collections.emptyMap());
                if (!dayMeals.isEmpty()) {
                    Map<String, List<PlannedMeal>> copyDay = new HashMap<>();
                    for (Map.Entry<String, List<PlannedMeal>> me : dayMeals.entrySet()) {
                        List<PlannedMeal> listCopy = new ArrayList<>();
                        for (PlannedMeal pm : me.getValue()) {
                            listCopy.add(new PlannedMeal(pm.recipe, pm.inventoryId));
                            if (pm.inventoryId != null && !pm.inventoryId.isEmpty()) {
                                origBatchUsage.computeIfAbsent(pm.inventoryId, k -> new ArrayList<>()).add(new PlannedMealRef(i, pm.recipe));
                            } else {
                                String rname = pm.recipe != null ? pm.recipe.recipe_name : "__unknown__";
                                recipeOnlyUsage.computeIfAbsent(rname, k -> new ArrayList<>()).add(i);
                            }
                        }
                        copyDay.put(me.getKey(), listCopy);
                    }
                    weekData.put(i, copyDay);
                }
            }

            if ("Overwrite (replace)".equals(mode)) {
                for (int i = 0; i < 7; i++) {
                    LocalDate d = dstSunday.plusDays(i);
                    String dKey = d.format(DATE_FORMAT);
                    mealPlans.remove(dKey);
                }
            }

            JSONArray invArr = readInventoryArray();
            Map<String, String> origToNewBatchId = new HashMap<>();
            Map<String, String> recipeToNewBatchId = new HashMap<>();

            for (Map.Entry<String, List<PlannedMealRef>> e : origBatchUsage.entrySet()) {
                String origId = e.getKey();
                InventoryEntry orig = findInventoryById(origId);
                InventoryEntry newBatch;
                if (orig != null) {
                    newBatch = new InventoryEntry();
                    newBatch.id = UUID.randomUUID().toString();
                    newBatch.name = orig.name;
                    newBatch.dateMade = orig.dateMade.plusDays(deltaDays);
                    newBatch.edibleDays = orig.edibleDays;
                    newBatch.totalPortions = orig.totalPortions;
                    newBatch.portionsUsed = 0;
                    newBatch.pricePerPortion = orig.pricePerPortion;
                    newBatch.notes = "Auto-created when copying week (from " + orig.id + ")";
                } else {
                    newBatch = new InventoryEntry();
                    newBatch.id = UUID.randomUUID().toString();
                    newBatch.name = "Copied batch";
                    newBatch.dateMade = srcSunday.plusDays(deltaDays);
                    newBatch.edibleDays = 3;
                    newBatch.totalPortions = Math.max(1, e.getValue().size());
                    newBatch.portionsUsed = 0;
                    newBatch.pricePerPortion = 0.0;
                    newBatch.notes = "Auto-created from missing original batch";
                }
                invArr.put(newBatch.toJson());
                origToNewBatchId.put(origId, newBatch.id);
            }

            for (Map.Entry<String, List<Integer>> e : recipeOnlyUsage.entrySet()) {
                String recipeName = e.getKey();
                Page4.Recipe r = findRecipeByName(recipeName);
                InventoryEntry nb = new InventoryEntry();
                nb.id = UUID.randomUUID().toString();
                nb.name = recipeName;
                int offset = e.getValue().get(0);
                LocalDate srcDateOfUsage = srcSunday.plusDays(offset);
                LocalDate newDateMade = srcDateOfUsage.plusDays(deltaDays);
                nb.dateMade = newDateMade;
                nb.edibleDays = r != null ? getEdibleDaysOrDefault(r) : 3;
                nb.totalPortions = r != null ? getTotalPortionsOrDefault(r) : Math.max(1, e.getValue().size());
                nb.portionsUsed = 0;
                nb.pricePerPortion = r != null ? r.cost_per_serving : 0.0;
                nb.notes = "Auto-created for copied week for recipe: " + recipeName;
                invArr.put(nb.toJson());
                recipeToNewBatchId.put(recipeName, nb.id);
            }

            writeInventoryArray(invArr);
            buildBatchLabels();

            for (Map.Entry<Integer, Map<String, List<PlannedMeal>>> e : weekData.entrySet()) {
                int offset = e.getKey();
                LocalDate dDay = dstSunday.plusDays(offset);
                String dKey = dDay.format(DATE_FORMAT);
                Map<String, List<PlannedMeal>> dstDayMap = mealPlans.getOrDefault(dKey, new HashMap<>());
                for (Map.Entry<String, List<PlannedMeal>> entry : e.getValue().entrySet()) {
                    String mealType = entry.getKey();
                    List<PlannedMeal> dstList = dstDayMap.getOrDefault(mealType, new ArrayList<>());
                    for (PlannedMeal pm : entry.getValue()) {
                        String newInvId = null;
                        if (pm.inventoryId != null && !pm.inventoryId.isEmpty()) {
                            newInvId = origToNewBatchId.get(pm.inventoryId);
                        } else {
                            String rname = pm.recipe != null ? pm.recipe.recipe_name : null;
                            if (rname != null) newInvId = recipeToNewBatchId.get(rname);
                        }
                        dstList.add(new PlannedMeal(pm.recipe, newInvId));
                    }
                    dstDayMap.put(mealType, dstList);
                }
                mealPlans.put(dKey, dstDayMap);
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(MEAL_PLAN_FILE))) {
                for (String date : mealPlans.keySet()) {
                    for (String meal : mealPlans.get(date).keySet()) {
                        for (PlannedMeal pm : mealPlans.get(date).get(meal)) {
                            String inv = pm.inventoryId != null ? pm.inventoryId : "";
                            pw.println(date + "|" + meal + "|" + pm.recipe.recipe_name + "|" + inv);
                        }
                    }
                }
            }

            openWeeklyPanel(dstSunday);
            JOptionPane.showMessageDialog(this, "Week copied successfully (new batches created for destination).", "Copy Week", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to copy week: " + ex.getMessage(), "Copy Week", JOptionPane.ERROR_MESSAGE);
        }
    }

    //==============================================================================================================
    // Small helpers & IO
    //==============================================================================================================
    private static class PlannedMealRef {
        int offset;
        Page4.Recipe recipe;
        PlannedMealRef(int offset, Page4.Recipe recipe) { this.offset = offset; this.recipe = recipe; }
    }

    private JSONArray readInventoryArray() {
        File f = new File(INVENTORY_FILE);
        if (!f.exists()) return new JSONArray();
        try {
            String s = new String(Files.readAllBytes(f.toPath()));
            return new JSONArray(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new JSONArray();
        }
    }

    private void writeInventoryArray(JSONArray arr) throws IOException {
        File f = new File(INVENTORY_FILE);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f, false)) {
            fw.write(arr.toString(2));
        }
    }

    private LocalDate getExpiryDateForPlanned(PlannedMeal pm) {
        if (pm == null) return null;
        Page4.Recipe r = pm.recipe;
        String invId = pm.inventoryId;
        if (invId != null) {
            InventoryEntry ie = findInventoryById(invId);
            if (ie != null) return ie.dateMade.plusDays(Math.max(0, ie.edibleDays));
        }
        Optional<LocalDate> earliest = findEarliestAssignmentDateForRecipe(r.recipe_name);
        int edible = getEdibleDaysOrDefault(r);
        return earliest.isPresent() ? earliest.get().plusDays(Math.max(0, edible)) : null;
    }

    private JPanel makeBigStatLabel(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(v, BorderLayout.EAST);
        p.add(l, BorderLayout.WEST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return p;
    }

    private int countAssignmentsOfRecipeInWeek(String recipeName, LocalDate weekStart) {
        int count = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            String ds = DATE_FORMAT.format(d);
            Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(ds, Collections.emptyMap());
            for (List<PlannedMeal> list : dayMeals.values()) {
                for (PlannedMeal pm : list) {
                    if (pm.recipe.recipe_name.equalsIgnoreCase(recipeName)) count++;
                }
            }
        }
        return count;
    }

    private Optional<LocalDate> findEarliestAssignmentDateForRecipe(String recipeName) {
        LocalDate earliest = null;
        for (String dateStr : mealPlans.keySet()) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FORMAT);
                Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(dateStr, Collections.emptyMap());
                for (List<PlannedMeal> list : dayMeals.values()) {
                    for (PlannedMeal pm : list) {
                        if (pm.recipe.recipe_name.equalsIgnoreCase(recipeName)) {
                            if (earliest == null || d.isBefore(earliest)) earliest = d;
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return Optional.ofNullable(earliest);
    }

    private int countAssignmentsOfRecipeUpToDate(String recipeName, LocalDate upToInclusive) {
        int count = 0;
        for (String dateStr : mealPlans.keySet()) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FORMAT);
                if (d.isAfter(upToInclusive)) continue;
                Map<String, List<PlannedMeal>> dayMeals = mealPlans.getOrDefault(dateStr, Collections.emptyMap());
                for (List<PlannedMeal> list : dayMeals.values()) {
                    for (PlannedMeal pm : list) {
                        if (pm.recipe.recipe_name.equalsIgnoreCase(recipeName)) count++;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return count;
    }

    //==============================================================================================================
    // Inventory helpers & counts
    //==============================================================================================================
    private List<InventoryEntry> loadInventoryEntries() {
        File f = new File(INVENTORY_FILE);
        if (!f.exists()) return Collections.emptyList();
        try {
            String s = new String(Files.readAllBytes(f.toPath()));
            JSONArray arr = new JSONArray(s);
            List<InventoryEntry> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) out.add(InventoryEntry.fromJson(arr.getJSONObject(i)));
            return out;
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    private InventoryEntry findInventoryById(String id) {
        if (id == null) return null;
        for (InventoryEntry ie : loadInventoryEntries()) {
            if (id.equals(ie.id)) return ie;
        }
        return null;
    }

    private int countAssignmentsOfInventory(String inventoryId) {
        if (inventoryId == null) return 0;
        int count = 0;
        for (Map<String, List<PlannedMeal>> m : mealPlans.values()) {
            for (List<PlannedMeal> list : m.values()) {
                for (PlannedMeal pm : list) {
                    if (inventoryId.equals(pm.inventoryId)) count++;
                }
            }
        }
        return count;
    }

    private int countAssignmentsOfInventoryUpToDate(String inventoryId, LocalDate upToInclusive) {
        if (inventoryId == null) return 0;
        int count = 0;
        for (String dateStr : mealPlans.keySet()) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FORMAT);
                if (d.isAfter(upToInclusive)) continue;
                Map<String, List<PlannedMeal>> m = mealPlans.getOrDefault(dateStr, Collections.emptyMap());
                for (List<PlannedMeal> list : m.values()) {
                    for (PlannedMeal pm : list) {
                        if (inventoryId.equals(pm.inventoryId)) count++;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return count;
    }

    private int countAssignmentsOfInventoryInRange(String inventoryId, LocalDate startInclusive, LocalDate endInclusive) {
        if (inventoryId == null || startInclusive == null || endInclusive == null) return 0;
        int count = 0;
        for (String dateStr : mealPlans.keySet()) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FORMAT);
                if (d.isBefore(startInclusive) || d.isAfter(endInclusive)) continue;
                Map<String, List<PlannedMeal>> m = mealPlans.getOrDefault(dateStr, Collections.emptyMap());
                for (List<PlannedMeal> list : m.values()) {
                    for (PlannedMeal pm : list) {
                        if (inventoryId.equals(pm.inventoryId)) count++;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return count;
    }

    private int inventoryRemainingPortions(String inventoryId) {
        InventoryEntry ie = findInventoryById(inventoryId);
        if (ie == null) return 0;
        int assigned = countAssignmentsOfInventory(inventoryId);
        int remaining = ie.totalPortions - ie.portionsUsed - assigned;
        return Math.max(0, remaining);
    }

    private void buildBatchLabels() {
        batchLabelMap.clear();
        List<InventoryEntry> inv = loadInventoryEntries();
        Map<String, Map<String, List<InventoryEntry>>> byRecipe = new HashMap<>();
        for (InventoryEntry ie : inv) {
            String r = ie.name == null ? "" : ie.name.toLowerCase();
            String dateKey = ie.dateMade.format(DISPLAY_BATCH_DATE);
            byRecipe.computeIfAbsent(r, k -> new HashMap<>())
                    .computeIfAbsent(dateKey, k -> new ArrayList<>())
                    .add(ie);
        }
        for (Map<String, List<InventoryEntry>> map : byRecipe.values()) {
            for (Map.Entry<String, List<InventoryEntry>> me : map.entrySet()) {
                List<InventoryEntry> list = me.getValue();
                list.sort(Comparator.comparing(a -> a.dateMade));
                for (int i = 0; i < list.size(); i++) {
                    InventoryEntry ie = list.get(i);
                    String label = me.getKey();
                    if (list.size() > 1) label = label + " (" + (i + 1) + ")";
                    batchLabelMap.put(ie.id, label);
                }
            }
        }
    }

    private String batchLabelOrShort(String inventoryId) {
        if (inventoryId == null) return null;
        if (batchLabelMap.containsKey(inventoryId)) return batchLabelMap.get(inventoryId);
        return inventoryId.length() > 6 ? inventoryId.substring(0, 6) : inventoryId;
    }

    //==============================================================================================================
    // Meal plan load/save
    //==============================================================================================================
    private void loadAllMealPlans() {
        mealPlans.clear();
        File file = new File(MEAL_PLAN_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts;
                if (line.contains("|")) parts = line.split("\\|", 4);
                else parts = line.split("\\t", 4);
                if (parts.length < 3) continue;
                String date = parts[0].trim();
                String mealType = parts[1].trim();
                String recipeName = parts[2].trim();
                String inventoryId = parts.length >= 4 ? parts[3].trim() : null;
                if (inventoryId != null && inventoryId.isEmpty()) inventoryId = null;

                recipeName = recipeName.replaceAll("^\"|\"$", "").trim();
                Page4.Recipe recipe = findRecipeByName(recipeName);
                if (recipe == null) {
                    recipe = findRecipeByName(recipeName.replace('_', ' '));
                    if (recipe == null) recipe = findRecipeByPartialName(recipeName);
                }
                if (recipe == null) recipe = createPlaceholderRecipe(recipeName);
                PlannedMeal pm = new PlannedMeal(recipe, inventoryId);
                mealPlans.computeIfAbsent(date, k -> new HashMap<>())
                        .computeIfAbsent(mealType, k -> new ArrayList<>())
                        .add(pm);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMealPlan(String dateStr, String mealType, List<PlannedMeal> selectedPlanned) {
        mealPlans.computeIfAbsent(dateStr, k -> new HashMap<>()).put(mealType, selectedPlanned);
        try (PrintWriter pw = new PrintWriter(new FileWriter(MEAL_PLAN_FILE))) {
            for (String date : mealPlans.keySet()) {
                for (String meal : mealPlans.get(date).keySet()) {
                    for (PlannedMeal pm : mealPlans.get(date).get(meal)) {
                        String inv = pm.inventoryId != null ? pm.inventoryId : "";
                        pw.println(date + "|" + meal + "|" + pm.recipe.recipe_name + "|" + inv);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //==============================================================================================================
    // Recipe helpers & misc
    //==============================================================================================================
    private Page4.Recipe findRecipeByName(String name) {
        List<Page4.Recipe> all = new ArrayList<>();
        all.addAll(loadRecipesFromFile("src/pages/text/recipes.txt"));
        all.addAll(loadRecipesFromFile("src/pages/text/custom_recipes.txt"));
        for (Page4.Recipe r : all) {
            if (r.recipe_name.equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private Page4.Recipe findRecipeByPartialName(String namePart) {
        List<Page4.Recipe> all = loadAllRecipes();
        String n = namePart.toLowerCase();
        for (Page4.Recipe r : all) {
            if (r.recipe_name.toLowerCase().contains(n)) return r;
        }
        return null;
    }

    private List<Page4.Recipe> loadAllRecipes() {
        List<Page4.Recipe> allRecipes = new ArrayList<>();
        allRecipes.addAll(loadRecipesFromFile("src/pages/text/custom_recipes.txt"));
        allRecipes.addAll(loadRecipesFromFile("src/pages/text/recipes.txt"));
        return allRecipes;
    }

    private List<Page4.Recipe> loadRecipesFromFile(String path) {
        Page4 temp = new Page4();
        return temp.loadRecipes(path);
    }

    private static ImageIcon loadImageIconStatic(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        Image img = icon.getImage();
        return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private int getTotalPortionsOrDefault(Page4.Recipe r) {
        try { return r.total_portions; } catch (Exception ex) { return 1; }
    }
    private int getEdibleDaysOrDefault(Page4.Recipe r) {
        try { return r.edible_days; } catch (Exception ex) { return 3; }
    }

    private Page4.Recipe createPlaceholderRecipe(String recipeName) {
        return new Page4.Recipe(
                0,
                recipeName,
                0.0,
                0.0,
                0.0,
                0.0,
                "",
                new ArrayList<>(),
                new ArrayList<>(),
                "Imported placeholder (recipe source not found)",
                ""
        );
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }
}