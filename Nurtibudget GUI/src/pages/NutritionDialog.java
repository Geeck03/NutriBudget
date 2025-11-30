package pages;

import javax.swing.*;
import java.awt.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class NutritionDialog {

    private static final Pattern SERVING_REGEX = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(g|kg|mg|ml|l|oz|fl\\s?oz|cup|cups|tbsp|tsp|lb|lbs)\\b", Pattern.CASE_INSENSITIVE);

    private NutritionDialog() {}

    public static void showNutritionDialog(Component ownerComponent, JSONObject sel) {
        if (sel == null) {
            JOptionPane.showMessageDialog(null, "No ingredient data available.");
            return;
        }

        String name = sel.optString("name", "Ingredient");
        Window owner = ownerComponent == null ? null : SwingUtilities.getWindowAncestor(ownerComponent);
        JDialog dialog = new JDialog(owner, "Nutrition — " + name, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 600);
        dialog.setLocationRelativeTo(ownerComponent);
        dialog.setLayout(new BorderLayout());
        JTextArea area = new JTextArea();
        area.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name).append("\n");

        String serving = sel.optString("serving_label", "");
        if (serving == null || serving.isEmpty()) {
            serving = extractServingLabelFromRaw(sel.optJSONObject("kroger_raw") != null ? sel.optJSONObject("kroger_raw") : sel);
        }
        sb.append("Serving: ").append(serving.isEmpty() ? "(unknown)" : serving).append("\n");

        double price = sel.optDouble("price", sel.optDouble("price_per_serving", 0.0));
        sb.append("Price: $").append(String.format("%.2f", price)).append("\n");

        double cals = sel.optDouble("calories_per_serving", 0.0);
        if (cals <= 0.0) {
            cals = extractCaloriesFromNutrients(sel);
            if (cals <= 0.0) {
                JSONObject raw = sel.optJSONObject("kroger_raw") != null ? sel.optJSONObject("kroger_raw") : sel;
                cals = raw != null ? raw.optDouble("calories", raw.optDouble("calories_per_serving", 0.0)) : 0.0;
            }
        }
        sb.append("Calories: ").append(String.format("%.2f", cals)).append(" kcal\n\n");

        if (sel.has("nutrients") && !sel.isNull("nutrients")) {
            Object obj = sel.opt("nutrients");
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                for (int i = 0; i < arr.length(); i++) sb.append(arr.get(i).toString()).append("\n");
            } else {
                sb.append(obj.toString());
            }
        } else if (sel.has("kroger_raw") && !sel.isNull("kroger_raw")) {
            sb.append("Raw nutrition data:\n").append(sel.optJSONObject("kroger_raw").toString(2)).append("\n");
        } else {
            sb.append("No nutrients available in this record.");
        }

        area.setText(sb.toString());
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(a -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static String extractServingLabelFromRaw(JSONObject raw) {
        if (raw == null) return "";
        String s = raw.optString("serving_label", "");
        if (!s.isEmpty()) return s;
        s = raw.optString("serving", "");
        if (!s.isEmpty()) return s;

        Object niObj = raw.opt("nutritionInformation");
        JSONObject nutri = null;
        if (niObj instanceof JSONObject) nutri = (JSONObject) niObj;
        else if (niObj instanceof JSONArray && ((JSONArray) niObj).length() > 0) nutri = ((JSONArray) niObj).optJSONObject(0);

        if (nutri != null) {
            Object servObj = nutri.opt("servingSize");
            JSONObject serv = null;
            if (servObj instanceof JSONObject) serv = (JSONObject) servObj;
            else if (servObj instanceof JSONArray && ((JSONArray) servObj).length() > 0) serv = ((JSONArray) servObj).optJSONObject(0);
            if (serv != null) {
                double qty = Double.NaN;
                if (serv.has("quantity")) qty = serv.optDouble("quantity", Double.NaN);
                else if (serv.has("value")) qty = serv.optDouble("value", Double.NaN);
                String unit = "";
                Object uom = serv.opt("unitOfMeasure");
                if (uom instanceof JSONObject) unit = ((JSONObject) uom).optString("abbreviation", ((JSONObject) uom).optString("name", ""));
                else if (uom instanceof String) unit = (String) uom;
                if (!Double.isNaN(qty)) {
                    String qtyStr = (qty % 1.0 == 0.0) ? Integer.toString((int) qty) : Double.toString(qty);
                    return (qtyStr + " " + unit).trim();
                }
            }
        }

        JSONArray items = raw.optJSONArray("items");
        if (items != null && items.length() > 0) {
            JSONObject item = items.optJSONObject(0);
            if (item != null) {
                String[] keys = new String[] { "size", "netContent", "packageSize", "displaySize", "sizeDescription", "measure", "packageSizeDescription" };
                for (String k : keys) {
                    String val = item.optString(k, "");
                    if (val != null && !val.isEmpty()) return val;
                }
                JSONArray sizes = item.optJSONArray("sizes");
                if (sizes != null && sizes.length() > 0) {
                    JSONObject s0 = sizes.optJSONObject(0);
                    if (s0 != null) {
                        String label = s0.optString("size", s0.optString("name", ""));
                        if (label != null && !label.isEmpty()) return label;
                    }
                }
            }
        }

        String[] textCandidates = new String[] { raw.optString("description", ""), raw.optString("receiptDescription", ""), raw.optString("displayName", ""), raw.optString("name", "") };
        for (String txt : textCandidates) {
            if (txt == null || txt.isEmpty()) continue;
            Matcher m = SERVING_REGEX.matcher(txt);
            if (m.find()) {
                String qty = m.group(1);
                String unit = m.group(2);
                return qty + " " + unit;
            }
        }
        return "";
    }

    private static double extractCaloriesFromNutrients(JSONObject sel) {
        if (sel == null) return 0.0;

        Object nutrients = sel.opt("nutrients");
        if (nutrients instanceof JSONArray) {
            double c = extractCaloriesFromNutrientsArray((JSONArray) nutrients);
            if (c > 0.0) return c;
        } else if (nutrients instanceof JSONObject) {
            JSONObject nobj = (JSONObject) nutrients;
            double c = nobj.optDouble("calories", -1.0);
            if (c >= 0.0) return c;
            Object inner = nobj.opt("items");
            if (inner instanceof JSONArray) {
                c = extractCaloriesFromNutrientsArray((JSONArray) inner);
                if (c > 0.0) return c;
            }
        }

        Object niObj = sel.opt("nutritionInformation");
        JSONObject nutri = null;
        if (niObj instanceof JSONObject) nutri = (JSONObject) niObj;
        else if (niObj instanceof JSONArray && ((JSONArray) niObj).length() > 0) nutri = ((JSONArray) niObj).optJSONObject(0);
        if (nutri != null) {
            Object n2 = nutri.opt("nutrients");
            if (n2 instanceof JSONArray) {
                double c = extractCaloriesFromNutrientsArray((JSONArray) n2);
                if (c > 0.0) return c;
            }
        }

        double top = sel.optDouble("calories", -1.0);
        if (top >= 0.0) return top;
        top = sel.optDouble("calories_per_serving", -1.0);
        if (top >= 0.0) return top;
        return 0.0;
    }

    private static double extractCaloriesFromNutrientsArray(JSONArray arr) {
        if (arr == null) return 0.0;
        final Pattern numberPat = Pattern.compile("(\\d+(?:\\.\\d+)?)");

        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.opt(i);
            if (item instanceof JSONObject) {
                JSONObject n = (JSONObject) item;
                String name = n.optString("displayName", n.optString("name", "")).toLowerCase();
                String code = n.optString("code", "").toLowerCase();
                if (name.contains("calor") || "calories".equals(code) || "cal".equals(code)) {
                    double q = n.optDouble("quantity", Double.NaN);
                    if (!Double.isNaN(q) && q >= 0.0) return q;
                    String qtyText = n.optString("quantity", n.optString("value", n.optString("label", "")));
                    if (qtyText != null && !qtyText.isEmpty()) {
                        Matcher m = numberPat.matcher(qtyText);
                        if (m.find()) {
                            try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                        }
                    }
                }
            } else if (item instanceof String) {
                String s = (String) item;
                String lower = s.toLowerCase();
                if (lower.contains("calor")) {
                    Matcher m = numberPat.matcher(s);
                    if (m.find()) {
                        try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                } else {
                    Matcher m = numberPat.matcher(s);
                    if (m.find() && (s.toLowerCase().contains("calor") || s.toLowerCase().contains("calories"))) {
                        try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                }
            }
        }
        return 0.0;
    }
}