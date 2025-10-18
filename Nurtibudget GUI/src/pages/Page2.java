package pages;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Page2 extends JPanel {

    // Ingredient class
    public static class Ingredient {
        public int id;
        public String name;
        public double cost;
        public int calories;
        public int protein;
        public int carbs;
        public double fat;

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        public Ingredient(int id, String name, double cost, int calories, int protein, int carbs, double fat) {
            this.id = id;
            this.name = name;
            this.cost = cost;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }
    }

    //==================================================================================================================

    public Page2() {
        setLayout(new BorderLayout());
        List<Ingredient> ingredients = loadIngredients("ingredients.txt");
        buildUI(ingredients);
    }

    //==================================================================================================================

    private void buildUI(List<Ingredient> ingredients) {
        JPanel gridPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 0;
        int col = 0;
        int row = 0;
        int maxCols = 3; //changes how many cards appear per row

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        for (Ingredient ing : ingredients) {
            gbc.gridx = col;
            gbc.gridy = row;
            gridPanel.add(createCard(ing), gbc);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.weighty = 1;
        gridPanel.add(Box.createVerticalGlue(), gbc);
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    //==================================================================================================================

    // Create the card for each ingredient
    private JPanel createCard(Ingredient ing) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        card.setBackground(new Color(250, 250, 250));
        card.setPreferredSize(new Dimension(180, 140));
        JLabel nameLabel = new JLabel(ing.name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        JLabel info = new JLabel(String.format(
                "<html><div style='text-align:center;'>"
                        + "Cost: $%.2f<br>"
                        + "Calories: %d kcal<br>"
                        + "Protein: %d g<br>"
                        + "Carbs: %d g<br>"
                        + "Fat: %.1f g"
                        + "</div></html>",
                ing.cost, ing.calories, ing.protein, ing.carbs, ing.fat
        ));

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(info);

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(230, 240, 255));
            }

            //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(250, 250, 250));
            }
        });

        return card;
    }

    //==================================================================================================================

    // Load ingredients from file with forgiving parsing
    private List<Ingredient> loadIngredients(String filePath) {
        List<Ingredient> ingredients = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header

            //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Split by tabs, filter out empty fields
                String[] parts = Arrays.stream(line.split("\t"))
                        .filter(p -> !p.trim().isEmpty())
                        .toArray(String[]::new);

                //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

                if (parts.length >= 7) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        double cost = Double.parseDouble(parts[2]);
                        int calories = Integer.parseInt(parts[3]);
                        int protein = Integer.parseInt(parts[4]);
                        int carbs = Integer.parseInt(parts[5]);
                        double fat = Double.parseDouble(parts[6]);

                        ingredients.add(new Ingredient(id, name, cost, calories, protein, carbs, fat));
                    }

                    catch (NumberFormatException e) {
                        System.err.println("Skipping bad line: " + line);
                    }
                }

                //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

                else {
                    System.err.println("Skipping malformed line: " + line);
                }
            }
        }

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        catch (IOException e) {
            e.printStackTrace();
        }
        return ingredients;
    }

    //==================================================================================================================

    // Standalone test window
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Ingredients Page");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 500);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new Page2());
            frame.setVisible(true);
        });
    }

    //==================================================================================================================

}
