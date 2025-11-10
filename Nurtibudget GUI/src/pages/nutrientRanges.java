package pages;


import java.util.HashMap;
import java.util.Map;

public class nutrientRanges {

    public static final Map<String, double[]> NUTRIENT_RANGES = new HashMap<>();

    static {
        // Format: {RDI, UL}

        // Vitamins
        NUTRIENT_RANGES.put("vitaminA", new double[]{900, 3000});
        NUTRIENT_RANGES.put("vitaminB1", new double[]{1.2, 50});
        NUTRIENT_RANGES.put("vitaminB2", new double[]{1.3, 50});
        NUTRIENT_RANGES.put("vitaminB3", new double[]{16, 35});
        NUTRIENT_RANGES.put("vitaminB5", new double[]{5, 1000});
        NUTRIENT_RANGES.put("vitaminB6", new double[]{1.3, 100});
        NUTRIENT_RANGES.put("vitaminB7", new double[]{30, 1000});
        NUTRIENT_RANGES.put("vitaminB9", new double[]{400, 1000});
        NUTRIENT_RANGES.put("vitaminB12", new double[]{2.4, 1000});
        NUTRIENT_RANGES.put("vitaminC", new double[]{90, 2000});
        NUTRIENT_RANGES.put("vitaminD", new double[]{20, 100});
        NUTRIENT_RANGES.put("vitaminE", new double[]{15, 1000});
        NUTRIENT_RANGES.put("vitaminK", new double[]{120, 1000});

        // Minerals
        NUTRIENT_RANGES.put("calcium", new double[]{1000, 2500});
        NUTRIENT_RANGES.put("phosphorus", new double[]{700, 4000});
        NUTRIENT_RANGES.put("magnesium", new double[]{400, 350}); // careful: UL applies to supplements
        NUTRIENT_RANGES.put("sodium", new double[]{1500, 2300});
        NUTRIENT_RANGES.put("potassium", new double[]{4700, 5000});
        NUTRIENT_RANGES.put("chloride", new double[]{2300, 3600});
        NUTRIENT_RANGES.put("sulfur", new double[]{1000, 2000});
        NUTRIENT_RANGES.put("iron", new double[]{18, 45});
        NUTRIENT_RANGES.put("zinc", new double[]{11, 40});
        NUTRIENT_RANGES.put("copper", new double[]{0.9, 10});
        NUTRIENT_RANGES.put("manganese", new double[]{2.3, 11});
        NUTRIENT_RANGES.put("iodine", new double[]{150, 1100});
        NUTRIENT_RANGES.put("selenium", new double[]{55, 400});
        NUTRIENT_RANGES.put("molybdenum", new double[]{45, 2000});
        NUTRIENT_RANGES.put("chromium", new double[]{35, 1000});
        NUTRIENT_RANGES.put("fluoride", new double[]{4, 10});
        NUTRIENT_RANGES.put("cobalt", new double[]{5, 100});

        // Macronutrients
        NUTRIENT_RANGES.put("fiber", new double[]{25, 70});
        NUTRIENT_RANGES.put("protein", new double[]{50, 200});
    }
}
