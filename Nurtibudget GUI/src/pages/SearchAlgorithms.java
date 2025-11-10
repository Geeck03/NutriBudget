package pages;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SearchAlgorithms {

    // Simple Search Function
    public static List<String> simpleSearch(String query, List<String> recipes) {
        final String lowerQuery = query.toLowerCase();
        return recipes.stream()
                .filter(recipe -> recipe.toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // Fuzzy Search Function (returns only names, no scores)
    public static List<String> fuzzySearch(String query, List<String> recipes, double threshold) {
        query = query.toLowerCase();
        List<String> results = new ArrayList<>();

        for (String recipe : recipes) {
            double ratio = Levenshtein.getSimilarity(query, recipe.toLowerCase());
            if (ratio > threshold) {
                results.add(recipe);  // store only the name
            }
        }

        return results;
    }

    // Smart Search (Exact match first, then Fuzzy search)
    public static List<String> smartSearch(String query, List<String> recipes) {
        List<String> exactResults = simpleSearch(query, recipes);

        if (!exactResults.isEmpty()) {
            return exactResults;  // Return exact matches if found
        }

        // Fallback to fuzzy search if no exact matches
        return fuzzySearch(query, recipes, 0.6); // threshold can be adjusted
    }

    public static class Levenshtein {

        public static int getLevenshteinDistance(String a, String b) {
            int lenA = a.length();
            int lenB = b.length();
            int[][] dp = new int[lenA + 1][lenB + 1];

            for (int i = 0; i <= lenA; i++) {
                for (int j = 0; j <= lenB; j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    } else if (j == 0) {
                        dp[i][j] = i;
                    } else {
                        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                        dp[i][j] = Math.min(
                                Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                                dp[i - 1][j - 1] + cost);
                    }
                }
            }
            return dp[lenA][lenB];
        }

        public static double getSimilarity(String a, String b) {
            int distance = getLevenshteinDistance(a, b);
            int maxLength = Math.max(a.length(), b.length());
            return 1.0 - (double) distance / maxLength;
        }
    }

    // Load recipes, only storing the name for search
    public static List<String> loadRecipes(String filename) {
        List<String> recipeNames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = line.split("\t");
                if (fields.length > 1) {
                    recipeNames.add(fields[1]); // store the name column only
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading recipes file: " + e.getMessage());
        }
        return recipeNames;
    }

    public static void main(String[] args) {

        List<String> recipes = loadRecipes("../recipes.txt");

        String query = "Chicken";
        System.out.println("Simple Search Results: " + simpleSearch(query, recipes));
        System.out.println("Fuzzy Search Results: " + fuzzySearch(query, recipes, 0.6));
        System.out.println("Smart Search Results: " + smartSearch(query, recipes));
    }
}
