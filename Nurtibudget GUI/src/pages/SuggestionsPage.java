package pages;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import py4j.ClientServer;
import bridge.IKrogerWrapper;

public class SuggestionsPage extends JPanel {

    private final Color OKSTATE_ORANGE = new Color(244, 125, 32);
    private final Color DARK_GRAY = new Color(45, 45, 45);
    private final Color LIGHT_GRAY = new Color(240, 240, 240);
    private final Color WHITE = Color.WHITE;

    private JPanel gridPanel;
    private JScrollPane scrollPane;

    private ClientServer clientServer;
    private IKrogerWrapper pythonEntry;

    public SuggestionsPage() {

        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        connectToPython();

        // =========== TITLE ===========
        JLabel title = new JLabel("Suggestions", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(DARK_GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(LIGHT_GRAY);
        titlePanel.add(title, BorderLayout.CENTER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(titlePanel, BorderLayout.NORTH);

        // =========== GRID AREA ===========
        gridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Load suggestions immediately
        SwingUtilities.invokeLater(this::loadSuggestions);
    }

    // =====================================================================
    // Python connection
    // =====================================================================
    private void connectToPython() {
        try {
            clientServer = new ClientServer(null);
            pythonEntry = (IKrogerWrapper) clientServer.getPythonServerEntryPoint(
                    new Class[]{IKrogerWrapper.class}
            );
            System.out.println("SuggestionsPage: Connected to Python");
        } catch (Exception e) {
            System.err.println("Failed to connect to Python");
            e.printStackTrace();
        }
    }

    // =====================================================================
    // Load favorites → send to python → build suggestions grid
    // =====================================================================
    private void loadSuggestions() {

        gridPanel.removeAll();

        JLabel loading = new JLabel("Generating suggestions...", SwingConstants.CENTER);
        loading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        gridPanel.setLayout(new BorderLayout());
        gridPanel.add(loading, BorderLayout.CENTER);
        gridPanel.revalidate();
        gridPanel.repaint();

        new Thread(() -> {
            try {
                JSONObject favoritesJson = collectFavorites();

                // CALL PYTHON AI
                String response = pythonEntry.generateSuggestions(favoritesJson.toString());
                JSONArray arr = new JSONArray(response);

                SwingUtilities.invokeLater(() -> {
                    buildGrid(arr);
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // =====================================================================
    // Collect favorites from Page2's stored files
    // =====================================================================
    private JSONObject collectFavorites() {
        JSONObject json = new JSONObject();
        JSONArray ingredientNames = new JSONArray();
        JSONArray recipeIds = new JSONArray();

        Path favoritesDir = Paths.get("src/pages/favorites");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(favoritesDir, "*.json")) {
            for (Path p : stream) {
                String s = Files.readString(p);
                JSONObject obj = new JSONObject(s);
                ingredientNames.put(obj.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        json.put("ingredients", ingredientNames);
        json.put("recipes", recipeIds); // if you later add recipes, this is ready

        return json;
    }

    // =====================================================================
    // Grid builder
    // =====================================================================
    private void buildGrid(JSONArray suggestions) {
        gridPanel.removeAll();
        gridPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 15, 15));

        for (int i = 0; i < suggestions.length(); i++) {
            JSONObject s = suggestions.getJSONObject(i);
            gridPanel.add(createSuggestionCard(s));
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createSuggestionCard(JSONObject s) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(200, 230));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel name = new JLabel(s.getString("name"), SwingConstants.CENTER);
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea desc = new JTextArea(s.getString("description"));
        desc.setLineWrap(true);
        desc.setEditable(false);
        desc.setBackground(Color.WHITE);

        card.add(name);
        card.add(Box.createVerticalStrut(8));
        card.add(desc);

        return card;
    }
}
