package bridge;

// Py4J interface exposed by the Python server. Methods must match Python implementation names/signatures.
public interface IKrogerWrapper {
    // existing search method (returns JSON string)
    String search(String query, int limit);

    // create recipe: returns integer id
    int createRecipe(String recipeName);

    // create ingredient: accepts ingredient JSON (or object) and returns an integer id
    int ingredientID(String ingredientJson);

    // Link an ingredient id to a recipe id with a given quantity.
    // Python signature should be: def addIngredientToRecipe(recipe_id: int, ingredient_id: int, quantity: float)
    void addIngredientToRecipe(int recipeId, int ingredientId, double quantity);

    // notification callbacks (optional)
    void on_new_recipe(String recipeJson);
    void on_new_ingredient(String ingredientJson);
    void on_recipe_added_ingredient(String recipeJson, String ingredientJson);
}