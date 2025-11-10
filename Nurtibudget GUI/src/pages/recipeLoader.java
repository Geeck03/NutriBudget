package pages;

import java.io.*;
import java.util.*;

public class recipeLoader {

    public static List<Page2.Recipe> loadRecipes(String filePath) {
        List<Page2.Recipe> list = new ArrayList<>();

        try (InputStream in = recipeLoader.class.getResourceAsStream(filePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t");

                int id = safeParseInt(p[0]);
                String name = p.length > 1 ? p[1] : "Unknown";
                double cost = p.length > 2 ? safeParseDouble(p[2]) : 0.0;
                int calories = p.length > 3 ? safeParseInt(p[3]) : 0;
                int protein = p.length > 4 ? safeParseInt(p[4]) : 0;
                int carbs = p.length > 5 ? safeParseInt(p[5]) : 0;
                double fat = p.length > 6 ? safeParseDouble(p[6]) : 0.0;
                String desc = p.length > 7 ? p[7] : "";
                String img = p.length > 8 ? p[8] : "";

                Page2.Recipe recipe = new Page2.Recipe(id, name, cost, calories, protein, carbs, fat, desc, img);

                // === Load optional nutrients (matches nutrientRanges.java) ===
                if (p.length > 9)  recipe.vitaminA = safeParseDouble(p[9]);
                if (p.length > 10) recipe.vitaminB1 = safeParseDouble(p[10]);
                if (p.length > 11) recipe.vitaminB2 = safeParseDouble(p[11]);
                if (p.length > 12) recipe.vitaminB3 = safeParseDouble(p[12]);
                if (p.length > 13) recipe.vitaminB5 = safeParseDouble(p[13]);
                if (p.length > 14) recipe.vitaminB6 = safeParseDouble(p[14]);
                if (p.length > 15) recipe.vitaminB7 = safeParseDouble(p[15]);
                if (p.length > 16) recipe.vitaminB9 = safeParseDouble(p[16]);
                if (p.length > 17) recipe.vitaminB12 = safeParseDouble(p[17]);
                if (p.length > 18) recipe.vitaminC = safeParseDouble(p[18]);
                if (p.length > 19) recipe.vitaminD = safeParseDouble(p[19]);
                if (p.length > 20) recipe.vitaminE = safeParseDouble(p[20]);
                if (p.length > 21) recipe.vitaminK = safeParseDouble(p[21]);

                if (p.length > 22) recipe.calcium = safeParseDouble(p[22]);
                if (p.length > 23) recipe.phosphorus = safeParseDouble(p[23]);
                if (p.length > 24) recipe.magnesium = safeParseDouble(p[24]);
                if (p.length > 25) recipe.sodium = safeParseDouble(p[25]);
                if (p.length > 26) recipe.potassium = safeParseDouble(p[26]);
                if (p.length > 27) recipe.chloride = safeParseDouble(p[27]);
                if (p.length > 28) recipe.sulfur = safeParseDouble(p[28]);
                if (p.length > 29) recipe.iron = safeParseDouble(p[29]);
                if (p.length > 30) recipe.zinc = safeParseDouble(p[30]);
                if (p.length > 31) recipe.copper = safeParseDouble(p[31]);
                if (p.length > 32) recipe.manganese = safeParseDouble(p[32]);
                if (p.length > 33) recipe.iodine = safeParseDouble(p[33]);
                if (p.length > 34) recipe.selenium = safeParseDouble(p[34]);
                if (p.length > 35) recipe.molybdenum = safeParseDouble(p[35]);
                if (p.length > 36) recipe.chromium = safeParseDouble(p[36]);
                if (p.length > 37) recipe.fluoride = safeParseDouble(p[37]);
                if (p.length > 38) recipe.cobalt = safeParseDouble(p[38]);
                if (p.length > 39) recipe.fiber = safeParseDouble(p[39]);

                // === Calculate NutriScore ===
                try {
                    Map<String, Object> scoreResult = nutriScore.calculateComprehensiveScore(recipe);
                    recipe.nutriGrade = (String) scoreResult.get("grade");
                } catch (Exception ex) {
                    System.err.println("NutriScore calculation failed for: " + name);
                    recipe.nutriGrade = "N/A";
                }

                list.add(recipe);
            }

        } catch (Exception e) {
            System.err.println("Error loading recipes from: " + filePath);
            e.printStackTrace();
        }

        return list;
    }

    private static double safeParseDouble(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0.0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int safeParseInt(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
