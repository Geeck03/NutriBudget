package bridge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper to call the Python bridge callbacks off the UI thread.
 * Uses the existing Py4JHelper to get the IKrogerWrapper.
 */
public final class PyBridgeInvoker {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pybridge-invoker");
        t.setDaemon(true);
        return t;
    });

    private PyBridgeInvoker() {}

    // ----- Notifications (fire-and-forget) -----
    public static void notifyNewRecipe(String recipeJson) {
        if (recipeJson == null) return;
        EXEC.submit(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper != null) wrapper.on_new_recipe(recipeJson);
            } catch (Throwable t) {
                System.err.println("notifyNewRecipe error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public static void notifyNewIngredient(String ingredientJson) {
        if (ingredientJson == null) return;
        EXEC.submit(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper != null) wrapper.on_new_ingredient(ingredientJson);
            } catch (Throwable t) {
                System.err.println("notifyNewIngredient error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public static void notifyRecipeAddedIngredient(String recipeJson, String ingredientJson) {
        if (recipeJson == null || ingredientJson == null) return;
        EXEC.submit(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper != null) wrapper.on_recipe_added_ingredient(recipeJson, ingredientJson);
            } catch (Throwable t) {
                System.err.println("notifyRecipeAddedIngredient error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // ----- Async createRecipe -----
    public static CompletableFuture<Integer> createRecipe(String recipeName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper == null) {
                    System.err.println("Py4J wrapper not available for createRecipe");
                    return -1;
                }
                return wrapper.createRecipe(recipeName);
            } catch (Throwable t) {
                System.err.println("createRecipe call failed: " + t.getMessage());
                t.printStackTrace();
                return -1;
            }
        }, EXEC);
    }

    // ----- Async createIngredient -----
    public static CompletableFuture<Integer> createIngredient(String ingredientJson) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper == null) {
                    System.err.println("Py4J wrapper not available for ingredientID");
                    return -1;
                }
                return wrapper.ingredientID(ingredientJson);
            } catch (Throwable t) {
                System.err.println("createIngredient call failed: " + t.getMessage());
                t.printStackTrace();
                return -1;
            }
        }, EXEC);
    }

    // ----- Fire-and-forget: addIngredientToRecipe -----
    public static void addIngredientToRecipe(int recipeId, int ingredientId, double quantity) {
        EXEC.submit(() -> {
            try {
                IKrogerWrapper wrapper = Py4JHelper.getWrapper();
                if (wrapper == null) {
                    System.err.println("Py4J wrapper not available for addIngredientToRecipe");
                    return;
                }
                wrapper.addIngredientToRecipe(recipeId, ingredientId, quantity);
            } catch (Throwable t) {
                System.err.println("addIngredientToRecipe call failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public static void shutdown() {
        try { EXEC.shutdownNow(); } catch (Throwable ignored) {}
    }
}