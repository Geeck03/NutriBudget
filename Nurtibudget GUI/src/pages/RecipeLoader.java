package pages;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.util.*;


public final class RecipeLoader {

    private RecipeLoader() {}

    public static List<Page4.Recipe> loadRecipesFromFile(String path) {
        List<Page4.Recipe> out = new ArrayList<>();
        File f = new File(path);
        if (!f.exists()) return out;
        String s;
        try {
            s = new String(Files.readAllBytes(f.toPath())).trim();
            if (s.isEmpty()) return out;
        } catch (IOException ex) {
            ex.printStackTrace();
            return out;
        }


        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Page4.Recipe r = Page4.Recipe.fromJson(o);
                out.add(r);
            }
            return out;
        } catch (Exception ignored) {}


        try (BufferedReader br = new BufferedReader(new StringReader(s))) {
            String header = br.readLine();
            if (header == null) return out;
            String sep = header.contains("\t") ? "\t" : ",";
            String[] cols = header.split(sep, -1);
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) {
                idx.put(cols[i].trim().toLowerCase(), i);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(sep, -1);

                String idStr = getCol(parts, idx, "ingredient_id", "id");
                String name = getCol(parts, idx, "name", "recipe_name", "title");
                String costStr = getCol(parts, idx, "cost", "price", "price_per_serving");
                String desc = getCol(parts, idx, "description", "describe", "notes");
                String image = getCol(parts, idx, "image_path", "imageurl", "image", "image_path");
                String calories = getCol(parts, idx, "calories", "kcal");

                Page4.Recipe r = new Page4.Recipe();
                r.recipe_ID = safeParseInt(idStr);
                r.recipe_name = (name != null && !name.isEmpty()) ? name : ("Recipe " + r.recipe_ID);
                r.cart_cost = safeParseDouble(costStr);
                r.cost_per_serving = safeParseDouble(costStr);
                r.recipe_cos_sum = r.cart_cost;
                r.description = desc != null ? desc : "";
                r.imagePath = image != null ? image : "";


                if (calories != null && !calories.isEmpty()) {
                    r.description = r.description + (r.description.isEmpty() ? "" : "\n") + "calories:" + calories;
                }
                out.add(r);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return out;
    }

    private static String getCol(String[] parts, Map<String,Integer> idx, String... names) {
        for (String n : names) {
            Integer i = idx.get(n.toLowerCase());
            if (i != null && i < parts.length) {
                String v = parts[i].trim();
                if (!v.isEmpty()) return v;
            }
        }
        return "";
    }


    public static String normalizeName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase();
        s = s.replace('_', ' ');
        s = s.replaceAll("[^\\p{Alnum} ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static int safeParseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double safeParseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
}