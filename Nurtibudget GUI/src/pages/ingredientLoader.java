package pages;

import java.io.*;
import java.util.*;

public class ingredientLoader {

    public static List<Page2.Ingredient> loadIngredients(String filePath) {
        List<Page2.Ingredient> list = new ArrayList<>();

        try (InputStream in = ingredientLoader.class.getResourceAsStream(filePath);
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

                Page2.Ingredient ing = new Page2.Ingredient(id, name, cost, calories, protein, carbs, fat, desc, img);

                // === Load optional nutrient values ===
                if (p.length > 9)  ing.vitaminA = safeParseDouble(p[9]);
                if (p.length > 10) ing.vitaminB1 = safeParseDouble(p[10]);
                if (p.length > 11) ing.vitaminB2 = safeParseDouble(p[11]);
                if (p.length > 12) ing.vitaminB3 = safeParseDouble(p[12]);
                if (p.length > 13) ing.vitaminB5 = safeParseDouble(p[13]);
                if (p.length > 14) ing.vitaminB6 = safeParseDouble(p[14]);
                if (p.length > 15) ing.vitaminB7 = safeParseDouble(p[15]);
                if (p.length > 16) ing.vitaminB9 = safeParseDouble(p[16]);
                if (p.length > 17) ing.vitaminB12 = safeParseDouble(p[17]);
                if (p.length > 18) ing.vitaminC = safeParseDouble(p[18]);
                if (p.length > 19) ing.vitaminD = safeParseDouble(p[19]);
                if (p.length > 20) ing.vitaminE = safeParseDouble(p[20]);
                if (p.length > 21) ing.vitaminK = safeParseDouble(p[21]);

                if (p.length > 22) ing.calcium = safeParseDouble(p[22]);
                if (p.length > 23) ing.phosphorus = safeParseDouble(p[23]);
                if (p.length > 24) ing.magnesium = safeParseDouble(p[24]);
                if (p.length > 25) ing.sodium = safeParseDouble(p[25]);
                if (p.length > 26) ing.potassium = safeParseDouble(p[26]);
                if (p.length > 27) ing.chloride = safeParseDouble(p[27]);
                if (p.length > 28) ing.sulfur = safeParseDouble(p[28]);
                if (p.length > 29) ing.iron = safeParseDouble(p[29]);
                if (p.length > 30) ing.zinc = safeParseDouble(p[30]);
                if (p.length > 31) ing.copper = safeParseDouble(p[31]);
                if (p.length > 32) ing.manganese = safeParseDouble(p[32]);
                if (p.length > 33) ing.iodine = safeParseDouble(p[33]);
                if (p.length > 34) ing.selenium = safeParseDouble(p[34]);
                if (p.length > 35) ing.molybdenum = safeParseDouble(p[35]);
                if (p.length > 36) ing.chromium = safeParseDouble(p[36]);
                if (p.length > 37) ing.fluoride = safeParseDouble(p[37]);
                if (p.length > 38) ing.cobalt = safeParseDouble(p[38]);
                if (p.length > 39) ing.fiber = safeParseDouble(p[39]);

                // === Calculate NutriScore ===
                try {
                    Map<String, Object> scoreResult = nutriScore.calculateComprehensiveScore(ing);
                    ing.nutriGrade = (String) scoreResult.get("grade");
                } catch (Exception ex) {
                    System.err.println("NutriScore calculation failed for: " + name);
                    ing.nutriGrade = "N/A";
                }

                list.add(ing);
            }

        } catch (Exception e) {
            System.err.println("Error loading ingredients from: " + filePath);
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
