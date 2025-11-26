package pages;

import java.util.List;

public class NutriScore {

    public static String gradeForRecipe(int calories, int protein, int carbs, double fat) {
        int neg = 0;
        neg += scoreCalories(calories);
        neg += scoreFat(fat);
        neg += scoreSugar(carbs);

        int pos = 0;
        pos += scoreProtein(protein);

        int finalScore = neg - pos;
        return convert(finalScore);
    }

    public static String gradeForIngredient(List<String> nutrientWords) {
        int neg = 0;
        int pos = 0;

        // Keyword-based scoring
        for (String n : nutrientWords) {
            String s = n.toLowerCase();

            if (s.contains("fiber") || s.contains("whole grain"))
                pos += 2;
            if (s.contains("protein"))
                pos += 1;

            if (s.contains("sugar") || s.contains("added sugar"))
                neg += 2;
            if (s.contains("fat") || s.contains("saturated"))
                neg += 2;
        }

        int finalScore = neg - pos;
        return convert(finalScore);
    }

    private static int scoreCalories(int kcal) {
        if (kcal < 80) return 0;
        if (kcal < 160) return 1;
        if (kcal < 240) return 2;
        if (kcal < 320) return 3;
        if (kcal < 400) return 4;
        if (kcal < 480) return 5;
        if (kcal < 560) return 6;
        if (kcal < 640) return 7;
        if (kcal < 720) return 8;
        if (kcal < 800) return 9;
        return 10;
    }

    private static int scoreFat(double fat) {
        return Math.min(10, (int)(fat / 3));   // 0–10
    }

    private static int scoreSugar(int carbs) {
        return Math.min(10, carbs / 5);        // crude sugar proxy
    }

    private static int scoreProtein(int protein) {
        return Math.min(5, protein / 5);       // 0–5
    }

    private static String convert(int score) {
        if (score <= -1) return "A";
        if (score <= 2) return "B";
        if (score <= 10) return "C";
        if (score <= 18) return "D";
        return "E";
    }
}
