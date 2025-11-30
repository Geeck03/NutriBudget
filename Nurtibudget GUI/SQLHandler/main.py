from MySQLHandler import MySQLHandler
from ingredient import Ingredient
from app import search_ingredients
from dotenv import load_dotenv
import os
import threading
import json
from py4j.clientserver import ClientServer
import time

# ===================== KrogerWrapper (unchanged) =====================
class KrogerWrapper:
    def __init__(self):
        print("✅ KrogerWrapper initialized using real Kroger API logic.")

    def search(self, query, limit):
        print(f"🔍 Python received query='{query}', limit={limit}")

        if not query.strip():
            return json.dumps([])

        try:
            ingredients = search_ingredients(query, limit)
            results = []

            for ing in ingredients:
                results.append({
                    "id": getattr(ing, "product_ID", None),
                    "name": getattr(ing, "name", "Unknown"),
                    "description": getattr(ing, "description", "No description"),
                    "price": getattr(ing, "local_regular", getattr(ing, "national_regular", 0)),
                    "promo_price": getattr(ing, "local_promo", getattr(ing, "national_promo", 0)),
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
            print(f"❌ Error in search(): {e}")
            return json.dumps({"error": str(e)})

# ===================== Py4J server setup =====================
def start_py4j_server(ready_event):
    wrapper = KrogerWrapper()
    server = ClientServer(
        java_parameters=JavaParameters(port=25333),
        python_parameters=PythonParameters(port=25334),
        python_server_entry_point=wrapper
    )
    print("✅ Python Py4J server running on ports 25333/25334")
    ready_event.set()  # <--- signal main thread

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("Py4J Server stopped.")

# ===================== Database Test (Optional) =====================
def test_database():
    """
    Inserts sample ingredients and a recipe to verify MySQL integration.
    """
    load_dotenv()
    host = os.getenv("host")
    user = os.getenv("user")
    password = os.getenv("password")
    database = os.getenv("database")
    
    
    
    #Initialize handler and connect
    db = MySQLHandler(
        host = host,
        user = user,
        password = password,
        database = database
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

    db.close()
    print("=== Database Connection Closed ===")

# ===================== Main =====================
if __name__ == "__main__":
    py4j_ready = threading.Event()
    py4j_thread = threading.Thread(target=start_py4j_server, args=(py4j_ready,), daemon=False)
    py4j_thread.start()

    py4j_ready.wait()
    print("✅ Py4J server ready, running main()...")

    main()

    # Keep Python alive for Java callbacks
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print(" Python process exiting")
