package pages;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CustomIngredientStore {
    private static final String CUSTOM_INGREDIENTS_FILE = "src/pages/text/custom_ingredients.json";

    public static List<CustomIngredient> loadAll() {
        File f = new File(CUSTOM_INGREDIENTS_FILE);
        if (!f.exists()) return new ArrayList<>();
        try {
            String s = new String(Files.readAllBytes(f.toPath()));
            JSONArray arr = new JSONArray(s);
            List<CustomIngredient> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(CustomIngredient.fromJson(o));
            }
            return out;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveAll(List<CustomIngredient> items) {
        try {
            JSONArray arr = new JSONArray();
            for (CustomIngredient ci : items) arr.put(ci.toJson());
            File f = new File(CUSTOM_INGREDIENTS_FILE);
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(f, false)) {
                fw.write(arr.toString(2));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}