package pages;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import pages.Page2.Ingredient;


public class nutriScore {

    public static int scoreNutrientBalance(double value, double rdi, double ul, int maxScore) {
        if (value < 0) return -maxScore;

        if (value >= rdi && value <= ul) return maxScore;

        if (value < rdi) {
            double percent = value / rdi;
            if (percent >= 0.8) return 0;
            else if (percent >= 0.6) return -1;
            else if (percent >= 0.4) return -2;
            else if (percent >= 0.2) return -3;
            else return -5;
        } else { // value > UL
            double percentExcess = (value - ul) / ul;
            if (percentExcess < 0.1) return 0;
            else if (percentExcess < 0.25) return -1;
            else if (percentExcess < 0.5) return -3;
            else return -5;
        }
    }

    public static String gradeFromScore(int score) {
        if (score >= 200) return "A+";
        else if (score >= 150) return "A";
        else if (score >= 100) return "B";
        else if (score >= 50) return "C";
        else if (score >= 0) return "D";
        else return "E";
    }

    public static Map<String, Object> calculateComprehensiveScore(Ingredient ingredient) {
        int totalScore = 0;
        Map<String, Map<String, Object>> details = new HashMap<>();

        for (Map.Entry<String, double[]> entry : nutrientRanges.NUTRIENT_RANGES.entrySet()) {
            String nutrient = entry.getKey();
            double rdi = entry.getValue()[0];
            double ul = entry.getValue()[1];

            double value;
            try {
                Method getter = Ingredient.class.getMethod("get" + capitalize(nutrient));
                value = (double) getter.invoke(ingredient);
            } catch (Exception e) {
                value = 0.0;
            }

            int score = scoreNutrientBalance(value, rdi, ul, 5);
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
