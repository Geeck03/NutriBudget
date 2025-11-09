from MySQLHandler import MySQLHandler
from ingredient import Ingredient
from app import search_ingredients

def main():
    """
    This is a test main method which adds Oats and Milk to the Ingredients table,
    and the recipe Overnight Oats to the recipe table with Milk and Oats linked to it.

    NOTE: As a ingregrity measure, MySQL cursor does not reset an AUTO_INCREMENTED value if a value from the table is deleted.
    For example, if you were to run the given test main method twice, then the ingredient table would look like this:
    1 Oats ...
    2 Milk ...
    3 Eggs ...
    4 Bread ...
    
    Then deleting these records will not reset the cursor, so if you run the test main method again,
    the ingredient_ID will start at 5 instead of 1.
    """

    #Initialize handler and connect
    db = MySQLHandler(
        host = "sql3.freesqldatabase.com",
        user = "sql3806244",
        password = "gE1lINQdeb",
        database ="sql3806244"
    )
    
    
    # Hope's test
    

    print ("\n=== Connected to Database ===")
    print("Attempt 2\n")

    # Insert sample ingredients

    oats = {
        "ingredient_name": "Oats",
        "calories": 389,
        "protein": 16.9,
        "carbs": 66.3,
        "fats": 6.9,
        "fiber": 10.6,
        "sugars": 0.0
    }

    milk = {
        "ingredient_name": "Milk",
        "calories": 42,
        "protein": 3.4,
        "carbs": 5.0,
        "fats": 1.0,
        "sugars": 5.0
    }

    '''
    oats_id = db.insert_ingredients(oats)
    milk_id = db.insert_ingredients(milk)

    print(f"Oats ID: {oats_id}, Milk ID: {milk_id}")

    # Insert recipe Overnight Oats

    print("\n=== Inserting Recipe ===")

    recipe = {
        "recipe_name": "Overnight Oats",
        "instructions": "Mix oats and milk in a jar. Refrigerate overnight and serve cold."
    }

    # PLEASE NOTE: Ingredients do not currently have a way to differenciate the unit type in quanity.
    # In the database table, selecting the quantity of an ingredient will only select the integer amount.
    # Later changes to the database shall include both a quantity and unit field.

    recipe_ingredients = [
        {"ingredient_ID": oats_id, "quantity": 50.0}, # 50 g oats
        {"ingredient_ID": milk_id, "quantity": 200.0} # 200 mL milk
    ]

    recipe_id = db.insert_recipe(recipe, recipe_ingredients)
    print(f"Recipe ID: {recipe_id}")

    print("\n=== Inserting Ingredient Object ===")

    # Add an Ingredient object 'Oats' to the database
    oats = Ingredient(
        product_ID="0001111089563",
        name="Oats",
        serving_size=40.0,
        serving_size_unit="g",
        servings_per_container=10,
        local_regular="$3.49",
        local_promo="$2.99",
        local_promo_per_unit_estimate="$0.07",
        local_regular_per_unit_estimate="$0.09",
        national_regular=3.49,
        national_promo=2.99,
        nutrients=[
            {"displayName": "Calories", "quantity": 389},
            {"displayName": "Protein", "quantity": 16.9},
            {"displayName": "Carbohydrate", "quantity": 66.3},
            {"displayName": "Total Fat", "quantity": 6.9},
            {"displayName": "Fiber", "quantity": 10.6},
            {"displayName": "Sugar", "quantity": 1.0},
            {"displayName": "Iron", "quantity": 4.7},
            {"displayName": "Magnesium", "quantity": 177}
        ]
    )
    '''
    print("\n=== Inserting Ingredient Object ===")
    
    apples = search_ingredients("bread", 2)
    apple = apples[0]
    

    ingredient_id = db.insert_ingredient_object(apple)
    print(f"Inserted Ingredient ID: {ingredient_id}")

    # Fetch data to verify
    '''
    print(f"\n=== Fetching Recipes ===")
    recipes = db.fetch_all("SELECT * FROM Recipes")
    for r in recipes:
        print(f"- {r['recipe_ID']}: {r['recipe_name']}")

    print("\n=== Fetching Linked Ingredients for 'Overnight Oats' ===")
    linked_ingredients = db.fetch_all("""
        SELECT R.recipe_name, I.ingredient_name, RI.quantity
        FROM RecipeIngredients RI
        JOIN Recipes R ON R.recipe_ID = RI.recipe_ID
        JOIN Ingredients I ON I.ingredient_ID = RI.ingredient_ID
        WHERE R.recipe_ID = %s
    """(recipe_id,))
    for li in linked_ingredients:
        print(f"• {li['ingredient_name']} ({li['quantity']} g/mL)")
    '''
    # Close the connection
    db.close()
    print(f"\n=== Database Connection Closed ===")

if __name__ == "__main__":
    main()