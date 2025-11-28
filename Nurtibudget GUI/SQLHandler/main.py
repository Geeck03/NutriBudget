from MySQLHandler import MySQLHandler
from ingredient import Ingredient
from app import search_ingredients
from app import createRecipe
from app import addConnectionBetweenIngredientAndRecipe
from app import addIngredientToRecipe
from dotenv import load_dotenv
import os
import threading
import json
import py4j
import app 
from py4j.clientserver import ClientServer
from py4j.clientserver import JavaParameters, PythonParameters

import time
import sys



server = None 
stop_event = threading.Event()
# ===================== KrogerWrapper (unchanged) =====================
class KrogerWrapper:
    
    ''' 
    Check for 
    '''
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        # older Pythons / unusual environments
        pass

    def __init__(self):
        print("✅ KrogerWrapper initialized using real Kroger API logic.")

    def search(self, query, limit):
        print(f"🔍 Python received: query='{query}', limit={limit}")

        if not query.strip():
            print("Empty search query received; returning empty list")
            return json.dumps([])

        try:
            ingredients = search_ingredients(query, limit)
            results = []

            for ing in ingredients:
                results.append({
                    "id": ing.product_ID,
                    "name": ing.name,
                    "describe": getattr(ing, "description", "No description available"),
                    "price": ing.local_regular if ing.local_regular else ing.national_regular,
                    "promo_price": ing.local_promo if ing.local_promo else ing.national_promo,
                    "image_url": getattr(ing, "image_url", ""),
                    "nutrients": getattr(ing, "sidebar_nutrients", [
                        f"Calories: {getattr(ing, 'calories', 'N/A')}",
                        f"Protein: {getattr(ing, 'protein', 'N/A')}g",
                        f"Carbs: {getattr(ing, 'carbs', 'N/A')}g",
                        f"Fat: {getattr(ing, 'fat', 'N/A')}g",
                    ])
                })

            return json.dumps(results, indent=2)

        except Exception as e:
            error_msg = f" Error in search(): {e}"
            print(error_msg)
            return json.dumps({"error": str(e)})

# ===================== Py4J server setup =====================
def start_py4j_server(ready_event, stop_event):
    global server
    wrapper = KrogerWrapper()
    server = ClientServer(python_server_entry_point=wrapper)
    print("✅ Python Py4J server running on default ports...")
    ready_event.set()
    try:
        while not stop_event.is_set(): 
            time.sleep(0.5)
    finally: 
        server.shutdown() 



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

    load_dotenv()  # Load environment variables from .env file
    host = os.getenv("host")
    user = os.getenv ("user")
    password = os.getenv("password")
    database = os.getenv("database")
    
    
    
    #Initialize handler and connect
    db = MySQLHandler(
        host = host,
        user = user,
        password = password,
        database = database
    )
    
     # Connect the app db to the main one 
    import app
    app.db = db
    
    # Hope's test
    

    print ("\n=== Connected to Database ===")
    print("Attempt 4\n")

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
    recipe_id = createRecipe("pie!")
    print(f"Inserted Recipe ID: {recipe_id}")
    addIngredientToRecipe(recipe_id, ingredient_id, 2.0)

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
    py4j_ready = threading.Event()
    py4j_thread = threading.Thread(target=start_py4j_server, args=(py4j_ready, stop_event), daemon=True)
    py4j_thread.start()

    py4j_ready.wait()
    print("✅ Py4J server ready, running main()...")

    main()

    # Keep Python alive for Java callbacks
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        stop_event.set()
