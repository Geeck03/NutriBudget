package pages;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;


class ResultCellRenderer extends JPanel implements ListCellRenderer<Object> {
    private final JLabel icon = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel meta = new JLabel();

    private static final int THUMB = 80;
    private static final ImageIcon PLACEHOLDER = (ImageIcon) UIManager.getIcon("FileView.fileIcon");

    private static final int IMAGE_CACHE_SIZE = 128;
    private static final Map<String, ImageIcon> imageCache = new LinkedHashMap<>(IMAGE_CACHE_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
            return size() > IMAGE_CACHE_SIZE;
        }
    };

    public ResultCellRenderer() {
        setLayout(new BorderLayout(12, 10));
        JPanel text = new JPanel(new GridLayout(0,1));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        meta.setFont(meta.getFont().deriveFont(Font.PLAIN, 12f));
        meta.setForeground(Color.DARK_GRAY);
        text.add(title);
        text.add(meta);
        icon.setPreferredSize(new Dimension(THUMB, THUMB));
        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        setBorder(new EmptyBorder(10,10,10,10));
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Reset
        title.setText("");
        meta.setText("");

        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            renderProduct(obj, list);
        } else if (value instanceof Page4.Recipe) {
            Page4.Recipe r = (Page4.Recipe) value;
            title.setText(r.recipe_name != null ? r.recipe_name : ("Recipe " + r.recipe_ID));
            meta.setText(r.description != null ? r.description : "");
            icon.setIcon(PLACEHOLDER);
        } else {

            title.setText(String.valueOf(value));
            meta.setText("");
            icon.setIcon(PLACEHOLDER);
        }

        setOpaque(true);
        setBackground(isSelected ? new Color(220,235,255) : Color.WHITE);
        return this;
    }

    private void renderProduct(JSONObject value, JList<?> list) {
        String name = value.optString("name", value.optString("description", "Unknown"));
        String desc = value.optString("describe", value.optString("description", ""));
        double price = value.optDouble("price", value.optDouble("price_per_serving", 0.0));
        String serving = value.optString("serving_label", value.optString("serving", ""));
        title.setText(name + (price > 0 ? String.format(" — $%.2f", price) : ""));
        meta.setText((desc != null ? desc : "") + (serving != null && !serving.isEmpty() ? " • " + serving : ""));

        String img = value.optString("image_url", "");
        if (img.isEmpty()) img = value.optString("imagePath", "");

        if (img == null || img.isEmpty()) {
            icon.setIcon(PLACEHOLDER);
            return;
        }

        synchronized (imageCache) {
            ImageIcon cached = imageCache.get(img);
            if (cached != null) {
                icon.setIcon(cached);
                return;
            }

            icon.setIcon(PLACEHOLDER);
            String finalImg = img;
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() {
                    try {
                        BufferedImage bi = ImageIO.read(new URL(finalImg));
                        if (bi == null) return PLACEHOLDER;
                        Image scaled = bi.getScaledInstance(THUMB, THUMB, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    } catch (Exception ex) {
                        return PLACEHOLDER;
                    }
                }
                @Override
                protected void done() {
                    try {
                        ImageIcon ic = get();
                        synchronized (imageCache) { imageCache.put(finalImg, ic); }

                        list.repaint();
                    } catch (Exception ignored) {}
                }
            }.execute();
        }
    }
}