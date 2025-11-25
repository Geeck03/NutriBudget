package pages;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/*
Overview:
- Inventory editor dialog: add/edit/remove/clear inventory batches.
- Calls provided refresh callback after changes so calendar reloads updated data.
- Important change/fix: do NOT force portionsUsed to include planned assignments when loading inventory.
  portionsUsed represents manual/actual used portions. Assignments from meal_plans are counted separately
  when computing remaining portions. If there are more assignments than available portions, the dialog will
  warn about over-assigned batches so the user can fix them.
*/

//======================================================================================================================
// InventoryDialog: fields & construction
//======================================================================================================================

public class InventoryDialog extends JDialog {

    //==============================================================================================================
    // Fields & Constants
    //==============================================================================================================
    private static final String INVENTORY_FILE = "src/pages/text/inventory.json";
    private static final String MEAL_PLAN_FILE = "src/pages/text/meal_plans.txt";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private InventoryTableModel tableModel;
    private JTable table;
    private final Runnable refreshCallback;

    //==============================================================================================================
    // Construction
    //==============================================================================================================
    public InventoryDialog(Frame owner) {
        this(owner, null);
    }

    public InventoryDialog(Frame owner, Runnable refreshCallback) {
        super(owner, "Inventory", true);
        this.refreshCallback = refreshCallback;
        setSize(820, 480);
        setLocationRelativeTo(owner);
        initUI();
        loadFromFile();
    }

    //==============================================================================================================
    // UI Initialization
    //==============================================================================================================
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new InventoryTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);

        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton addBtn = new JButton("Add (from recipes)");
        JButton editBtn = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");
        JButton clearAllBtn = new JButton("Clear all");
        JButton saveBtn = new JButton("Save");

        addBtn.addActionListener(e -> {
            Page4.Recipe selected = openRecipePicker();
            if (selected != null) {
                InventoryItem it = InventoryItem.createFromRecipe(selected);
                if (openEditDialog(it)) {
                    tableModel.add(it);
                }
            }
        });

        editBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                InventoryItem it = tableModel.getAt(modelRow);
                if (openEditDialog(it)) tableModel.fireTableRowsUpdated(modelRow, modelRow);
            } else {
                JOptionPane.showMessageDialog(this, "Select a row to edit.");
            }
        });

        removeBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                InventoryItem it = tableModel.getAt(modelRow);
                int confirm = JOptionPane.showConfirmDialog(this, "Remove selected item and its calendar assignments?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String removedId = it.id;
                    tableModel.removeAt(modelRow);
                    try {
                        saveToFile();
                        removeAssignmentsForInventoryId(removedId);
                        loadFromFile();
                        if (refreshCallback != null) refreshCallback.run();
                        JOptionPane.showMessageDialog(this, "Removed item and cleared related calendar assignments.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Failed to remove item: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Select a row to remove.");
            }
        });

        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Clear all inventory batches and remove any related calendar assignments? This cannot be undone.",
                    "Confirm clear all",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                File f = new File(INVENTORY_FILE);
                if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                try (FileWriter fw = new FileWriter(f, false)) { fw.write(new JSONArray().toString(2)); }
                removeAllInventoryAssignmentsFromMealPlans();
                loadFromFile();
                if (refreshCallback != null) refreshCallback.run();
                JOptionPane.showMessageDialog(this, "Cleared all inventory and removed associated calendar entries.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to clear inventory: " + ex.getMessage());
            }
        });

        saveBtn.addActionListener(e -> {
            try {
                saveToFile();
                if (refreshCallback != null) refreshCallback.run();
                JOptionPane.showMessageDialog(this, "Saved inventory to " + INVENTORY_FILE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            }
        });

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(removeBtn);
        actions.add(clearAllBtn);
        actions.add(saveBtn);

        root.add(actions, BorderLayout.NORTH);

        JTextArea info = new JTextArea("Manage your inventory batches.\nClick Add to pick a recipe and create a batch pre-filled from the recipe.\nClear all will remove all inventory batches and also remove any calendar items that referenced those batches.");
        info.setBackground(root.getBackground());
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setBorder(new EmptyBorder(6,6,6,6));
        root.add(info, BorderLayout.SOUTH);

        setContentPane(root);
    }

    //==============================================================================================================
    // Meal-plan file operations affecting inventory assignments
    //==============================================================================================================
    private void removeAssignmentsForInventoryId(String invId) {
        File f = new File(MEAL_PLAN_FILE);
        if (!f.exists()) return;
        List<String> kept = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.contains("|") ? line.split("\\|", 4) : line.split("\\t", 4);
                if (parts.length >= 4) {
                    String id = parts[3].trim();
                    if (id.equals(invId)) continue;
                }
                kept.add(line);
            }
        } catch (IOException ex) { ex.printStackTrace(); }
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            for (String l : kept) pw.println(l);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void removeAllInventoryAssignmentsFromMealPlans() {
        File f = new File(MEAL_PLAN_FILE);
        if (!f.exists()) return;
        List<String> kept = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.contains("|") ? line.split("\\|", 4) : line.split("\\t", 4);
                if (parts.length >= 4) {
                    String id = parts[3].trim();
                    if (!id.isEmpty()) continue;
                }
                kept.add(line);
            }
        } catch (IOException ex) { ex.printStackTrace(); }
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            for (String l : kept) pw.println(l);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    //==============================================================================================================
    // Small helpers (recipe picker / edit dialog / table model / io)
    //==============================================================================================================
    private Page4.Recipe openRecipePicker() {
        List<Page4.Recipe> all = loadAllRecipes();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No recipes available to pick from.");
            return null;
        }
        final Page4.Recipe[] selected = new Page4.Recipe[1];
        JDialog d = new JDialog(this, "Select Recipe", true);
        d.setSize(800, 600);
        d.setLocationRelativeTo(this);
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(container);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        for (Page4.Recipe r : all) {
            JPanel card = new JPanel(new BorderLayout(10,8));
            card.setBorder(new EmptyBorder(8,8,8,8));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

            JLabel img = new JLabel();
            ImageIcon ic = loadImageIconStatic(r.imagePath, 100, 80);
            if (ic != null) img.setIcon(ic);
            else { img.setText("[No Image]"); img.setForeground(Color.GRAY); img.setPreferredSize(new Dimension(100, 80)); }
            card.add(img, BorderLayout.WEST);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            JLabel name = new JLabel("<html><b>" + r.recipe_name + "</b></html>");
            JLabel meta = new JLabel(String.format("Cost/serv: $%.2f  |  Portions: %d  |  Edible days: %d", r.cost_per_serving, getTotalPortionsOrDefault(r), getEdibleDaysOrDefault(r)));
            center.add(name); center.add(meta);
            card.add(center, BorderLayout.CENTER);

            JButton select = new JButton("Select");
            select.addActionListener(e -> { selected[0] = r; d.dispose(); });
            card.add(select, BorderLayout.EAST);

            container.add(card);
            container.add(Box.createVerticalStrut(6));
        }

        d.add(sp, BorderLayout.CENTER);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> d.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancel);
        d.add(bottom, BorderLayout.SOUTH);
        d.setVisible(true);
        return selected[0];
    }

    private boolean openEditDialog(InventoryItem item) {
        JDialog d = new JDialog(this, item.id == null ? "Add Inventory" : "Edit Inventory", true);
        d.setSize(520, 380);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JTextField nameF = new JTextField(item.name, 30);
        JTextField dateF = new JTextField(item.dateMade != null ? item.dateMade.format(DATE_FORMAT) : LocalDate.now().format(DATE_FORMAT), 12);
        JTextField edibleF = new JTextField(String.valueOf(item.edibleDays), 4);
        JTextField totalF = new JTextField(String.valueOf(item.totalPortions), 4);
        JTextField usedF = new JTextField(String.valueOf(item.portionsUsed), 4);
        JTextField priceF = new JTextField(String.format("%.2f", item.pricePerPortion), 8);
        JTextArea notesA = new JTextArea(item.notes != null ? item.notes : "", 4, 30);

        p.add(new JLabel("Name:")); p.add(nameF); p.add(Box.createVerticalStrut(6));
        p.add(new JLabel("Date Made (yyyy-MM-dd):")); p.add(dateF); p.add(Box.createVerticalStrut(6));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.add(new JLabel("Edible days:")); row.add(edibleF); row.add(Box.createHorizontalStrut(10));
        row.add(new JLabel("Total portions:")); row.add(totalF); row.add(Box.createHorizontalStrut(10));
        row.add(new JLabel("Portions used:")); row.add(usedF);
        p.add(row);
        p.add(Box.createVerticalStrut(6));
        p.add(new JLabel("Cost per portion ($):")); p.add(priceF);
        p.add(Box.createVerticalStrut(6));
        p.add(new JLabel("Notes:")); p.add(new JScrollPane(notesA));
        p.add(Box.createVerticalStrut(8));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        btns.add(cancel); btns.add(save); p.add(btns);

        final boolean[] saved = {false};
        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            try {
                String name = nameF.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Name required");
                LocalDate pd = LocalDate.parse(dateF.getText().trim(), DATE_FORMAT);
                int edible = Math.max(0, Integer.parseInt(edibleF.getText().trim()));
                int total = Math.max(1, Integer.parseInt(totalF.getText().trim()));
                int used = Math.max(0, Integer.parseInt(usedF.getText().trim()));
                double price = Double.parseDouble(priceF.getText().trim());
                used = Math.min(used, total);
                item.name = name; item.dateMade = pd; item.edibleDays = edible; item.totalPortions = total;
                item.portionsUsed = Math.min(used, total); item.pricePerPortion = price; item.notes = notesA.getText().trim();
                if (item.id == null) item.id = UUID.randomUUID().toString();
                saved[0] = true; d.dispose();
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Invalid value: " + ex.getMessage()); }
        });

        d.setContentPane(p);
        d.setVisible(true);
        return saved[0];
    }

    private static class InventoryItem {
        public String id;
        public String name;
        public LocalDate dateMade;
        public int edibleDays;
        public int totalPortions;
        public int portionsUsed;
        public double pricePerPortion;
        public String notes;

        static InventoryItem createDefault() {
            InventoryItem it = new InventoryItem();
            it.id = null; it.name = ""; it.dateMade = LocalDate.now();
            it.edibleDays = 3; it.totalPortions = 1; it.portionsUsed = 0; it.pricePerPortion = 0.0; it.notes = "";
            return it;
        }

        static InventoryItem createFromRecipe(Page4.Recipe r) {
            InventoryItem it = new InventoryItem();
            it.id = null; it.name = r.recipe_name; it.dateMade = LocalDate.now();
            it.edibleDays = Math.max(0, r != null ? r.edible_days : 3);
            it.totalPortions = Math.max(1, r != null ? r.total_portions : 1);
            it.portionsUsed = 0; it.pricePerPortion = Math.max(0.0, r != null ? r.cost_per_serving : 0.0);
            it.notes = "Created from recipe: " + (r != null ? r.recipe_name : "");
            return it;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("name", name); o.put("dateMade", dateMade.format(DATE_FORMAT));
            o.put("edibleDays", edibleDays); o.put("totalPortions", totalPortions); o.put("portionsUsed", portionsUsed);
            o.put("pricePerPortion", pricePerPortion); o.put("notes", notes != null ? notes : "");
            return o;
        }

        static InventoryItem fromJson(JSONObject o) {
            InventoryItem it = new InventoryItem();
            it.id = o.optString("id", UUID.randomUUID().toString());
            it.name = o.optString("name", "");
            it.dateMade = LocalDate.parse(o.optString("dateMade", LocalDate.now().format(DATE_FORMAT)), DATE_FORMAT);
            it.edibleDays = o.optInt("edibleDays", 3);
            it.totalPortions = o.optInt("totalPortions", 1);
            it.portionsUsed = o.optInt("portionsUsed", 0);
            it.pricePerPortion = o.optDouble("pricePerPortion", 0.0);
            it.notes = o.optString("notes", "");
            return it;
        }
    }

    private static class InventoryTableModel extends AbstractTableModel {
        private final String[] cols = {"Name", "Date Made", "EdibleDays", "Total", "Used", " $/portion", "Notes"};
        private final List<InventoryItem> items = new ArrayList<>();

        public void setList(List<InventoryItem> list) { items.clear(); items.addAll(list); fireTableDataChanged(); }
        public List<InventoryItem> getList() { return items; }
        public void add(InventoryItem it) { items.add(it); fireTableRowsInserted(items.size()-1, items.size()-1); }
        public void removeAt(int idx) { items.remove(idx); fireTableRowsDeleted(idx, idx); }
        public InventoryItem getAt(int idx) { return items.get(idx); }

        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }
        @Override public Object getValueAt(int row, int col) {
            InventoryItem it = items.get(row);
            switch (col) {
                case 0: return it.name;
                case 1: return it.dateMade.format(DATE_FORMAT);
                case 2: return it.edibleDays;
                case 3: return it.totalPortions;
                case 4: return it.portionsUsed;
                case 5: return String.format("%.2f", it.pricePerPortion);
                case 6: return it.notes;
                default: return "";
            }
        }
    }

    private void loadFromFile() {
        try {
            File f = new File(INVENTORY_FILE);
            if (!f.exists()) { tableModel.setList(Collections.emptyList()); return; }
            String s = new String(Files.readAllBytes(f.toPath()));
            JSONArray arr = new JSONArray(s);
            List<InventoryItem> out = new ArrayList<>();
            Map<String, Integer> assignedMap = computeAssignmentsFromMealPlans();

            // collect any over-assigned IDs to warn the user
            List<String> overAssigned = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                InventoryItem it = InventoryItem.fromJson(arr.getJSONObject(i));
                // Do NOT force portionsUsed to include assigned counts.
                it.portionsUsed = Math.max(0, Math.min(it.portionsUsed, it.totalPortions));
                int assigned = assignedMap.getOrDefault(it.id, 0);
                if (assigned > (it.totalPortions - it.portionsUsed)) {
                    overAssigned.add(String.format("%s (id:%s) assigned:%d total:%d used:%d",
                            it.name, it.id, assigned, it.totalPortions, it.portionsUsed));
                }
                out.add(it);
            }

            tableModel.setList(out);

            if (!overAssigned.isEmpty()) {
                StringBuilder sb = new StringBuilder("Warning: some batches are over-assigned (planned meals exceed available portions). Please review:\n\n");
                for (String row : overAssigned) sb.append(row).append("\n");
                JOptionPane.showMessageDialog(this, sb.toString(), "Over-assigned batches", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Failed to load inventory: " + ex.getMessage()); }
    }

    private void saveToFile() throws IOException {
        JSONArray arr = new JSONArray();
        for (InventoryItem it : tableModel.getList()) { it.portionsUsed = Math.max(0, Math.min(it.portionsUsed, it.totalPortions)); arr.put(it.toJson()); }
        File f = new File(INVENTORY_FILE);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f, false)) { fw.write(arr.toString(2)); }
    }

    private Map<String, Integer> computeAssignmentsFromMealPlans() {
        Map<String, Integer> map = new HashMap<>();
        File f = new File(MEAL_PLAN_FILE);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.contains("|") ? line.split("\\|", 4) : line.split("\\t", 4);
                if (parts.length < 4) continue;
                String invId = parts[3].trim();
                if (invId.isEmpty()) continue;
                map.put(invId, map.getOrDefault(invId, 0) + 1);
            }
        } catch (IOException ex) { ex.printStackTrace(); }
        return map;
    }

    private List<Page4.Recipe> loadAllRecipes() {
        List<Page4.Recipe> all = new ArrayList<>();
        try {
            Page4 p = new Page4();
            all.addAll(p.loadRecipes("src/pages/text/custom_recipes.txt"));
            all.addAll(p.loadRecipes("src/pages/text/recipes.txt"));
        } catch (Exception ex) { ex.printStackTrace(); }
        return all;
    }

    private static ImageIcon loadImageIconStatic(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        Image img = icon.getImage();
        return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private int getTotalPortionsOrDefault(Page4.Recipe r) { try { return r.total_portions; } catch (Exception ex) { return 1; } }
    private int getEdibleDaysOrDefault(Page4.Recipe r) { try { return r.edible_days; } catch (Exception ex) { return 3; } }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryDialog d = new InventoryDialog(null);
            d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            d.setVisible(true);
        });
    }
}