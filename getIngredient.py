import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format
import json
from typing import List, Optional, Dict, Any

# Our Files 
from ingredient import Ingredient # Ingredient data class
from ingredient import nutrientInfo # nutrientInfo data class
from accessToken import getAccessToken 

'''
Search Ingredients Functionality
Expected Input: name of ingredient (string) and search number (int) (expected number of results)
Expected Output: List of Ingredient data class objects
'''

# Search for ingredients by name and location ID and search_number (desired number of results)

def search_ingredients(name: str, search_number: int) -> List[Ingredient]:
    search_url = "https://api.kroger.com/v1/products"
    
    
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
            servings_per_package = servings_per_package[0] if servings_per_package else {}
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
        #serving_size_description = serving_dict.get("description", "") or "No Description"
        
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
            
    

        '''
        Getting the price information 
        
        '''
        
        # Takes the first item/SKU
        items_list = product.get("items") or [] 
        item: Dict[str, Any] = items_list[0] if items_list else{} 
        
        '''
        #allergens 
        allergen = product.get("allergens") or [] 
        
        
        if isinstance(allergen, dict): 
            allergen = allergen
        elif isinstance(allergen, str):
            allergen = allergen[0] if allergen else {}
        else:
            allergen = {} 
    
        # print(allergen)
        '''
    
        # Local price block (dict or {})
        price = item.get("price", {}) or {} 
        # print(price)
        local_regular = price.get("regular")
        local_regular_per_unit_estimate = price.get("regularPerUnitEstimate") 
        local_promo = price.get("promo")
        local_promo_per_unit_estimate = price.get("promoPerUnitEstimate")

        # National price block (dict or {})
        nprice = item.get("nationalPrice", {}) or {}
        national_regular = nprice.get("regular")
        national_promo = nprice.get("promo")
        national_promo_per_unit_estimate = nprice.get("promoPerUnitEstimate")
        national_regular_per_unit_estimate = nprice.get("regularPerUnitEstimate")
        
        
        ingredient = Ingredient(
            name=name_of_product,
            product_ID=product_id,
            servings_per_container=servings_per_container,
            serving_size=serving_size_quantity,
            #serving_size_description=serving_size_description,
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
        ingredients_class_list.append(ingredient)
        nutrient_objs = []  # Clear nutrient objects list for next ingredient
    return ingredients_class_list
