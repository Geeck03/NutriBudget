import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from getIngredient import search_ingredients
#from getIngredient import get_price

from ingredient import Ingredient
from accessToken import getAccessToken
from locationCall import location_ID

from typing import List, Optional, Dict, Any


'''
Test Search Ingredients Functionality
'''


# Location ID for Oklahoma City, OK: 540FC253

def main():
    
    ingredients: List[Ingredient] = []
    ingredients = search_ingredients("oats", 10)
    
    # Print ingredient descriptions
    print("Number of ingredients found: ", len(ingredients))
    for i in range (len(ingredients)): 
        print("Ingredient ", i+1, ": ", ingredients[i].describe())
        print("\n")
        
if __name__ == "__main__":
    main()
