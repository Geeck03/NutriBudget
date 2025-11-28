from dotenv import load_dotenv
import os
import math 
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from dataclasses import asdict
from dataclasses import dataclass, field
from typing import List
import json
from typing import List, Optional, Dict, Any
from ingredient import Ingredient 
from ingredient import nutrientInfo
from MySQLHandler import MySQLHandler   # Your DB wrapper

# List of all the nutrient fields in the ingredient and recipe database 
NUTRIENT_FIELDS = [
    "calories", "protein", "carbs", "fats", "fiber", "sugars",
    "vitamin_a", "vitamin_c", "vitamin_d",
    "calcium", "iron", "potassium",
    "sodium", "magnesium", "cholesterol",
    "saturated_fat", "trans_fat"
]



'''
Listed functions in order they are present: 
def getAccessToken(): 
    Returns access token 
def location_ID(zipcode: str) -> str: 
    Takes Zip code of a kroger store and returns the store's ID. 
    Can be used as a parameter for the search_ingredient function 
    for a store's local price data.
def search_ingredients(name: str, search_number: int) -> List[Ingredient]:
    Takes a search query as a string, and a search number (the amount of 
    results you want). Reuturns a list of ingredient objects pertaining to the 
    search query. Each ingredient object is filled with the information in the 
    ingredients class. 
def scaleIngredient(ing: Ingredient, scale: int) -> Ingredient: 
    Modifies an ingredent's nutritional values based on the scale given. 
    Returns a modified version of the ingredient. 

# Create Recipe Scripts 
def createRecipe(recipe_name: str = "") -> int:  
    UI| Used when create recipe button is pressed 
    Creates a new recipe given a recipe name.
    All values are initialized to zero 
    returns recipe ID 

def ingredientID(ingredient_obj: Any) -> int:
    UI| Used when the add to recipe button is clicked from 
    the create recipe page. 
    Takes an ingredient name, and returns the ingredient ID. 
    Creates a new ingredient in the ingredient Table or pulls 
    from existing entry. 
    
def addIngredientToRecipe(recipe_id: int, ingredient_id: int, quantity: float):
    UI| Page: Edit recipe - > add ingredient -> add to recipe -> 
    promopted to add quantity on submit call this function. 
    Takes a recipe id and ingredient id. Does all the
    functionality to add all the ingredients to the functions 
    Calls: 
    def ScaleIngredientFunction(ingredient_dict: Dict[str, Any], quantity: float) -> Dict[str, Any]:
    def ingredientID(ingredient_obj: Any) -> int:
    
def UpdateCostPerServing(recipe_id: int): 
    Helper to add Ingredient 
    Upates the cost per serving when addIngredientToRecipe is called  
    

'''


''' 
Create connection
'''

# Initialize DB handler
db = MySQLHandler(
    host="localhost",
    database="NBDB",
    user="root",
    password="password"
)

''''
Access Token code
'''

def getAccessToken():
    load_dotenv()  # Load environment variables from .env file
    CLIENT_ID = os.getenv("CLIENT_ID")
    CLIENT_SECRET = os.getenv("CLIENT_SECRET")

    auth = base64.b64encode(f"{CLIENT_ID}:{CLIENT_SECRET}".encode()).decode()
    token_url = "https://api.kroger.com/v1/connect/oauth2/token"

    data = {"grant_type": "client_credentials","scope": "product.compact"}
    headers = {"Content-Type": "application/x-www-form-urlencoded","Authorization": f"Basic {auth}"}

    response = requests.post(token_url, headers=headers, data=data)
  
    if response.status_code == 200: 
        print("Success")
        token = response.json()
        access_token = token.get("access_token")
        #print("Access Token:", access_token)
    elif response.status_code != 200:
        print("Error", response.status_code, response.text)
        
    access_token = response.json().get("access_token")
    #print("Access Token:", access_token)

    return access_token 

'''
Takes a zip code of a store and returns the store ID. The store ID
is needed for the search function to get local price data.
'''
def location_ID(zipcode: str) -> str:
    search_url = "https://api.kroger.com/v1/locations" 

    token: str = ""

    token = getAccessToken() 

    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}"
    }

    params = {
        "filter.zipCode.near": zipcode,
        "filter.limit": 1
    }

    
    locationID = requests.get(search_url, headers=headers, params=params)

    if locationID.status_code == 200: 
        print("Success in location ID retrieval")
    else :
        print("Error", locationID.status_code, locationID.text)
        return "Error in Location ID retrieval"
    
    return locationID.json()["data"][0]["locationId"]


'''
Search Ingredients Functionality
Expected Input: name of ingredient (string) and search number (int) (expected number of results)
Expected Output: List of Ingredient data class objects
'''

# Search for ingredients by name and location ID and search_number (desired number of results)

def search_ingredients(name: str, search_number: int) -> List[Ingredient]:
    search_url = "https://api.kroger.com/v1/products"
    
    # --- Empty search guard ---
    if not name.strip():
        print("Empty search query received; returning empty list")
        return []
    
    '''
     For location ID, we can either hardcode a value for testing or
     implement a function to get location ID based on zip code.
     '''
    # Add Location id based on zip code later 
    location_ID = "03400397"  # Example location ID 
    
    # Edage case: if search number is less than or equal to 0, return empty list
    if search_number <= 0:
        return []
    
    '''
    API call 
    '''    
    
    params = {
        "filter.term": name,
        "filter.limit": search_number,
        "filter.locationId": location_ID # Example location ID
    } 

    token: str = ""
    token = getAccessToken()
    
    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}"
    }

    response = requests.get(search_url, headers=headers, params=params)

    if response.status_code != 200:
        print("Error", response.status_code, response.text)
        return []  # Return an empty Ingredient on error 
    
    
    '''
    Parse Response and create Ingredient objects
    '''
    
    data = response.json()  # Get list of json responses. Kroger returns two dicts: "meta" and "data"
    products = data.get("data", []) or [] # Get list of products from "data" key


    # Edge case: no products found
    if not products:
        print("No products found.")
        return []
    
    # Create list to hold Ingredient objects
    ingredients_class_list: List[Ingredient] = []
    
    # Limit results to search_number or available products whatever number is smaller  
    limit = min(search_number, len(products))


    ''' 
    Loop through each product in the response and extract relevant information
    '''
    
    for i in range (limit):
        product = products[i] # Extract single product dict
        product_id = product.get("productId", "") # Get product ID key value 
        # Get display name if there is not display name, use recipet description, if neither use name
        name_of_product = product.get("description") or product.get("receiptDescription") or name 

        # Dictionary object filled with nutrient information
        nutri_info = product.get("nutritionInformation") or {}
        
       
        # Normalize response: Sometimes kroger returns a list another times a dict. Make sure we always have a dict to work with
        if isinstance(nutri_info, dict): 
            nutri_info = nutri_info
        elif isinstance(nutri_info, list):
            nutri_info = nutri_info[0] if nutri_info else {}
        else:
            nutri_info = {}
            
        servings_per_package = nutri_info.get("servingsPerPackage", 0.0) or 0.0
        
        if isinstance(servings_per_package, dict): 
            servings_per_package = servings_per_package
        elif isinstance(servings_per_package, float):
            servings_per_package = servings_per_package if servings_per_package else {}
        else:
            servings_per_package = {}
            
        servings_per_container = servings_per_package.get("value", 0.0) or 0.0
            
        
        # Get the serving size information
        serving_dict = nutri_info.get("servingSize", {}) or {}
        if isinstance(serving_dict, dict): 
            serving_dict = serving_dict
        elif isinstance(serving_dict, list):
            serving_dict = serving_dict[0] if serving_dict else {}
        else:
            serving_dict = {}
        serving_size_quantity = serving_dict.get("quantity", 0.0) or 0.0
        
        
        serving_size_unit = serving_dict.get("unitOfMeasure", "") or ""
        
        if isinstance(serving_size_unit, dict): 
            serving_size_unit = serving_size_unit
        elif isinstance(serving_size_unit, str):
            serving_size_unit = serving_size_unit[0] if serving_size_unit else {}
        else:
            serving_size_unit = {} 
        # print(serving_size_unit)
        serving_size_name = serving_size_unit.get("abbreviation", "") or serving_size_unit.get("name", "") or "Unknown serving size unit"
            
        # Get list of nutrients    
        nutrients_list = nutri_info.get("nutrients", []) or []
        
        # Create list to hold nutrientInfo objects
        nutrient_objs: List[nutrientInfo] = []
        
        # Side bar nutrients 
        sidebar_nutrients: List[str] = []
          
        ''' 
        Loop through each nutrient and create nutrientInfo objects 
        '''
        
        for nutrient in nutrients_list:
           # Unit of measure extraction
           unit_of_measure = (nutrient.get("unitOfMeasure") or {}).get("name", "")
           unit = unit_of_measure.get("name", "") if isinstance(unit_of_measure, dict) else str(unit_of_measure)
                
                
           displayName = nutrient.get("displayName") or nutrient.get("code") or "Unknown"
           code = nutrient.get("code", "")
           description = nutrient.get("description", "")
           qty = nutrient.get("quantity")
           unit = (nutrient.get("unitOfMeasure") or {}).get("abbreviation") or (nutrient.get("unitOfMeasure") or {}).get("name") or ""
           pdv = nutrient.get("percentDailyIntake")
                
           # Create nutrientInfo object
           nutrient = nutrientInfo(
              displayName=displayName,
              code=code, 
              description=description,
              quantity=qty,
              percentDailyIntake=pdv,
              unitOfMeasure=unit, 
            )                
                
    
           # Add nutrient object to list
           # Will add this list to the Ingredient data class later
           nutrient_objs.append(nutrient)
           
           
           # Add to sidebar_nutrients list 
           sidebar_nutrients.append(nutrient.describe())
            
        '''
        Image information 
        
        '''
        

        # Image: take first available image from product images if exists
        image_url = ""
        images_list = product.get("images") or []
        if images_list:
            first_image = images_list[0]
            image_url = first_image.get("sizes", [{}])[0].get("url", "") if first_image.get("sizes") else first_image.get("url", "")

    

        '''
        Getting the price information 
        '''
        
        # Takes the first item/SKU
        items_list = product.get("items") or [] 
        item: Dict[str, Any] = items_list[0] if items_list else{} 
        
        # Local price block (dict or {})
        price = item.get("price", {}) or {} 
        local_regular = price.get("regular") or 0.0
        local_regular_per_unit_estimate = price.get("regularPerUnitEstimate") or 0.0
        local_promo = price.get("promo") or 0.0
        local_promo_per_unit_estimate = price.get("promoPerUnitEstimate") or 0.0

        # National price block (dict or {})
        nprice = item.get("nationalPrice", {}) or {}
        national_regular = nprice.get("regular") or 0.0
        national_promo = nprice.get("promo") or 0.0
        national_promo_per_unit_estimate = nprice.get("promoPerUnitEstimate") or 0.0
        national_regular_per_unit_estimate = nprice.get("regularPerUnitEstimate") or 0.0
        
        
        ingredient = Ingredient(
            name=name_of_product,
            product_ID=product_id,
            servings_per_container=servings_per_container,
            serving_size=serving_size_quantity,
            serving_size_unit=serving_size_name,
            nutrients = nutrient_objs,
            local_regular=local_regular,
            local_regular_per_unit_estimate = local_regular_per_unit_estimate,
            local_promo=local_promo,
            local_promo_per_unit_estimate= local_promo_per_unit_estimate,
            national_regular=national_regular,
            national_promo=national_promo,
            national_promo_per_unit_estimate=national_promo_per_unit_estimate,
            national_regular_per_unit_estimate=national_regular_per_unit_estimate
        )
        
        
        # Assign extra fields after creation
        ingredient.image_url = image_url
        ingredient.sidebar_nutrients = sidebar_nutrients
        
        ingredients_class_list.append(ingredient)
        nutrient_objs = []  # Clear nutrient objects list for next ingredient
    return ingredients_class_list


'''
    (OLD)
    Scale the ingredient object given based on the number given such as 2/3, 3, or 0.5 
    Then return the ingredient object that was modified 

def scaleIngredient(ing: Ingredient, scale: int) -> Ingredient: 
     
     # Scales quanity and percentDailyIntake based on the scale 
     for n in ing.nutrients: 
         n.quantity = n.quantity * scale if n.quantity else 0 
         n.percentDailyIntake = n.percentDailyIntake * scale if n.percentDailyIntake else 0 
         
     return ing

Create a New Recipe and return its ID
UI| Used when create recipe button is clicked 
'''

def createRecipe(recipe_name: str = "") -> int: 
    '''
    Create a new recipe row in the Recipe table. 
    All values are set as zero
    Returns the recipe_ID assigned by the db 
    '''

    ingredient_id = []

    initial_values = { 
        "recipe_name": recipe_name, 
        "num_ingredients": 0, 
        "ingredient_cost_sum": 0, 
        "cost_cook": 0, 
        "cost_per_serving": 0,
        "cart_cost": 0, 
        "nutrition_grade": None, 
        "instructions": ""
    }

    recipe_id = db.insert_recipe(initial_values, ingredient_id)
    return recipe_id 

'''
Get the ingredient ID 
UI| On the add ingredient page the user clicks add ingredient. 
'''
def ingredientID(ingredient_obj: Any) -> int: 
    '''
    Checks if the ingredient is already in the Ingredient Table. 
    If so, return its ID 
    Else, insert the ingredient into the db and return the new ID 
    '''

    # Check for the ingredient in db 
    existing = db.fetch_one( 
        "SELECT ingredient_ID FROM Ingredients WHERE ingredient_name = %s",
        (ingredient_obj.name,)

    )

    if existing: 
        return existing["ingredient_ID"]
    
    # Insert the new ingredient if needed 

    data = ingredient_obj
    new_id = db.insert_ingredients("Ingredients", data)
    return new_id 

'''
Below functions are needed for the to addIngredientToRecipe
ScaleIngredientFunction 
addConnectionBetweenIngredientAndRecipe

'''

'''
Scales ingredient based on a given quantity 
Takes ingredient dict scales 
'''

def scaleIngredientFunction(ingredient_dict: Dict[str, Any], quantity: float) -> Dict[str, Any]: 
    '''
    Returns a new dictionary containing nutrient values scaled by quantity. 
    Ex. 
        calories_og = 10 
        quantity = 2 
        calories * quantity = 20 
    '''
    scaled = ingredient_dict.copy() 

    for nutrient in NUTRIENT_FIELDS:
        if nutrient in ingredient_dict and ingredient_dict[nutrient] is not None:
            scaled[nutrient] = float(ingredient_dict[nutrient]) * quantity
    return scaled 


''' 
Creates RecipeIngredient table and initializes it with given paramenters 
'''

def addConnectionBetweenIngredientAndRecipe(recipe_id: int, ingredient_id: int, quantity: float) -> None:
    '''
    
    '''
    ingredient = db.fetch_one(
        "SELECT quantity_unit FROM Ingredients WHERE ingredient_ID = %s",
        (ingredient_id,)
    )

    
    db.insert("RecipeIngredient", {
        "recipe_ID": recipe_id,
        "ingredient_ID": ingredient_id,
        "quantity": quantity,
        "quantity_unit": ingredient["quantity_unit"]
    })

'''
Adds ingredient to recipe and makes connection between 
ingredient and the recipe and creates the recipeIngredient table 
'''
def addIngredientToRecipe(recipe_id: int, ingredient_id: int, quantity: float): 
    '''
    Connects and ingredient to a recipe by creating a
    recipeIngredient table. 
    Takes the recipe_id and ingredient id 

    Scales Ingredient 
    Updates 

    1. Pulls ingredient from db, creating a copy of ingredient 
    and the nutritional fields based on the quantity paramter 
    2. Add ingredient's nutritional values to recipe's existing values in the recipe database 
    3. Update the ingredient count in the recipeIngredient db 
    4. Add the recipeIngredient connection row 
    5. Use Kevin's future code to grab code 
    6. Update price values 
    '''

    # Step 1: Get ingredient from DB and scale a copy of it 

    ing = db.fetch_one( 
        "SELECT * FROM Ingredients WHERE ingredient_ID = %s", 
        (ingredient_id,)
    )
    
    if not ing: 
        raise ValueError("Ingredient not found in database")
    
    
    scaled = scaleIngredientFunction(ing, quantity) 

    # Step 2: Add ingredient's nutritional values to recipe's existing values in the recipe database 
    nutrient_updates = {field: scaled[field] for field in NUTRIENT_FIELDS}
    db.update_increment("Recipes", "recipe_ID", recipe_id, nutrient_updates) 
    
    
    # Step 3: Update the ingredient count in the recipeIngredient db 
    db.update_increment("Recipes", "recipe_ID", recipe_id, {"num_ingredients": 1})


    # Step 4: Add the recipeIngredient connection row 
    
    addConnectionBetweenIngredientAndRecipe(recipe_id, ingredient_id, quantity)
    
    
    ''' 
    Step 5: We will later use Kevin's function to take a recipe have it's nutrtional 
    values evaluted. His function returns a string that we will then store in the 
    recipe db. The string is a letter representing the grade of the food 
    '''

    # Step 6: Update price values 
    
    recipe = db.fetch_one( 
        "SELECT * FROM Recipes WHERE recipe_ID = %s", 
        (recipe_id,)
    )
    
    if not recipe: 
        raise ValueError("Ingredient not found in database")
    
    # promo 
    promo = ing["promo"]
    ingredient_cost = promo * quantity # cost of ingredient 
    cart_cost = math.ceil(ingredient_cost)
    
    # Call update cost per serving 
    UpdateCostPerServing(recipe_id) # Call UpdateCostPerServing function 
    
    db.update_increment("Recipes", "recipe_ID", recipe_id, {"ingredient_cost_sum": ingredient_cost})
    db.update_increment("Recipes", "recipe_ID", recipe_id, {"cart_cost": cart_cost})
'''
Updates the cost per serving

'''
def UpdateCostPerServing(recipe_id: int): 
    
    
    recipe = db.fetch_one( 
        "SELECT * FROM Recipes WHERE recipe_ID = %s", 
        (recipe_id,)
    )
    
    if not recipe: 
        raise ValueError("Ingredient not found in database")
    
    
    query = """
    SELECT serving_size
    FROM recipes
    WHERE recipe_ID = %s AND serving_size IS NOT NULL AND serving_size <> 0;
    """

    result = db.fetch_one(query, (recipe_id,))

    if result:
        serving_size = result[0]
        
        
    else:
        serving_size = None

    query = """
        UPDATE recipes
        SET cost_per_serving = %s
        WHERE recipe_ID = %s;
    """
    
    ingredient_cost_sum = recipe["ingredient_cost_sum"]        
    new_cost_per_serving = ingredient_cost_sum / serving_size # calculate 
    db.execute_query(query, (new_cost_per_serving, recipe_id))