package pages;

import java.util.HashMap;
import java.util.Map;

public class nutriScore {

    // Compute points for a nutrient (beneficial only)
    public static int scoreNutrient(double value, double rdi, double ul) {
        if (value <= 0) return 0;
        double percentOfRDI = value / rdi;
        if (percentOfRDI >= 1) return 5;
        if (percentOfRDI >= 0.8) return 4;
        if (percentOfRDI >= 0.6) return 3;
        if (percentOfRDI >= 0.4) return 2;
        if (percentOfRDI >= 0.2) return 1;
        return 0;
    }

    // Map total points to NutriScore grade
    public static String gradeFromScore(int score) {
        if (score >= 50) return "A";
        else if (score >= 35) return "B";
        else if (score >= 20) return "C";
        else if (score >= 10) return "D";
        else return "E";
    }

    // Calculate NutriScore for an Ingredient or Recipe
    public static Map<String, Object> calculateComprehensiveScore(Object item) {
        int totalScore = 0;
        Map<String, Map<String, Object>> details = new HashMap<>();

        for (Map.Entry<String, double[]> entry : nutrientRanges.NUTRIENT_RANGES.entrySet()) {
            String nutrient = entry.getKey();
            double rdi = entry.getValue()[0];
            double ul = entry.getValue()[1];

            double value;
            try {
                var field = item.getClass().getDeclaredField(nutrient); // <— use item.getClass()
                field.setAccessible(true);
                value = (double) field.get(item);
            } catch (Exception e) {
                value = 0.0;
                System.out.println("Missing nutrient " + nutrient + " in class " + item.getClass().getName());
            }

                

            int score = scoreNutrient(value, rdi, ul);
            totalScore += score;

            Map<String, Object> nutrientDetails = new HashMap<>();
            nutrientDetails.put("value", value);
            nutrientDetails.put("score", score);
            details.put(nutrient, nutrientDetails);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("score", totalScore);
        result.put("grade", gradeFromScore(totalScore));
        result.put("details", details);

        return result;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
