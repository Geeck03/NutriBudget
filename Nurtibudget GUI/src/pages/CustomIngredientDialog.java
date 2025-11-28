package pages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class CustomIngredientDialog extends JDialog {
    private final DefaultListModel<CustomIngredient> listModel = new DefaultListModel<>();
    private final JList<CustomIngredient> list = new JList<>(listModel);

    // callback when user selects an ingredient (accepts selected CustomIngredient)
    private final Consumer<CustomIngredient> onSelect;

    public CustomIngredientDialog(Window owner, Consumer<CustomIngredient> onSelect) {
        super(owner, "Custom Ingredients", ModalityType.APPLICATION_MODAL);
        this.onSelect = onSelect;
        initUI();
        loadList();
        setSize(640, 480);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(8,8,8,8));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        root.add(sp, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton selectBtn = new JButton("Select");
        JButton closeBtn = new JButton("Close");
        controls.add(addBtn);
        controls.add(editBtn);
        controls.add(deleteBtn);
        controls.add(selectBtn);
        controls.add(closeBtn);

        root.add(controls, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            CustomIngredient sel = list.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select an item to edit."); return; }
            openEditor(sel);
        });
        deleteBtn.addActionListener(e -> {
            CustomIngredient sel = list.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select an item to delete."); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + sel.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            listModel.removeElement(sel);
            persistList();
        });
        selectBtn.addActionListener(e -> doSelect());
        closeBtn.addActionListener(e -> dispose());

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) doSelect();
            }
        });

        setContentPane(root);
    }

    private void loadList() {
        listModel.clear();
        List<CustomIngredient> items = CustomIngredientStore.loadAll();
        for (CustomIngredient ci : items) listModel.addElement(ci);
    }

    private void persistList() {
        List<CustomIngredient> items = Collections.list(listModel.elements());
        CustomIngredientStore.saveAll(items);
    }

    private String makeUniqueName(String base) {
        if (base == null) base = "";
        String candidate = base.trim();
        if (candidate.isEmpty()) candidate = "Ingredient";
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < listModel.size(); i++) {
            CustomIngredient c = listModel.get(i);
            if (c.name != null) existing.add(c.name.toLowerCase());
        }
        if (!existing.contains(candidate.toLowerCase())) return candidate;
        int idx = 1;
        String next;
        do {
            next = candidate + " (" + idx + ")";
            idx++;
        } while (existing.contains(next.toLowerCase()));
        return next;
    }

    private void openEditor(CustomIngredient existing) {
        CustomIngredient ci = existing != null ? existing : new CustomIngredient();
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        p.add(new JLabel("Name:"), gc);
        gc.gridx = 1;
        JTextField nameF = new JTextField(ci.name, 30);
        p.add(nameF, gc);

        gc.gridx = 0; gc.gridy = 1;
        p.add(new JLabel("Serving label:"), gc);
        gc.gridx = 1;
        JTextField servingF = new JTextField(ci.serving_label, 20);
        p.add(servingF, gc);

        gc.gridx = 0; gc.gridy = 2;
        p.add(new JLabel("Unit:"), gc);
        gc.gridx = 1;
        JTextField unitF = new JTextField(ci.unit, 10);
        p.add(unitF, gc);

        gc.gridx = 0; gc.gridy = 3;
        p.add(new JLabel("Price per serving ($):"), gc);
        gc.gridx = 1;
        JTextField priceF = new JTextField(String.format("%.2f", ci.price_per_serving), 10);
        p.add(priceF, gc);

        gc.gridx = 0; gc.gridy = 4;
        p.add(new JLabel("Calories per serving:"), gc);
        gc.gridx = 1;
        JTextField calF = new JTextField(String.format("%.1f", ci.calories_per_serving), 10);
        p.add(calF, gc);

        gc.gridx = 0; gc.gridy = 5;
        p.add(new JLabel("Notes:"), gc);
        gc.gridx = 1;
        JTextField notesF = new JTextField(ci.notes, 30);
        p.add(notesF, gc);

        int res = JOptionPane.showConfirmDialog(this, p, existing == null ? "Add custom ingredient" : "Edit custom ingredient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String newName = nameF.getText().trim();
        if (newName.isEmpty()) { JOptionPane.showMessageDialog(this, "Name required."); return; }

        // ensure unique name when adding a new ingredient
        if (existing == null) {
            String unique = makeUniqueName(newName);
            ci.name = unique;
            ci.serving_label = servingF.getText().trim();
            ci.unit = unitF.getText().trim();
            try { ci.price_per_serving = Double.parseDouble(priceF.getText().trim()); } catch (Exception ex) { ci.price_per_serving = 0.0; }
            try { ci.calories_per_serving = Double.parseDouble(calF.getText().trim()); } catch (Exception ex) { ci.calories_per_serving = 0.0; }
            ci.notes = notesF.getText().trim();
            listModel.addElement(ci);
            persistList();
        } else {
            // editing existing - if name changed, ensure it doesn't collide with other names
            String candidate = newName;
            if (!candidate.equalsIgnoreCase(existing.name)) {
                candidate = makeUniqueName(candidate);
            }
            existing.name = candidate;
            existing.serving_label = servingF.getText().trim();
            existing.unit = unitF.getText().trim();
            try { existing.price_per_serving = Double.parseDouble(priceF.getText().trim()); } catch (Exception ex) { existing.price_per_serving = 0.0; }
            try { existing.calories_per_serving = Double.parseDouble(calF.getText().trim()); } catch (Exception ex) { existing.calories_per_serving = 0.0; }
            existing.notes = notesF.getText().trim();
            list.repaint();
            persistList();
        }
    }

    private void doSelect() {
        CustomIngredient sel = list.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select an ingredient first."); return; }
        if (onSelect != null) onSelect.accept(sel);
        dispose();
    }
}