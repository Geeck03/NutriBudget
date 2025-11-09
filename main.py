import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from getIngredient import search_ingredients
#from getIngredient import get_price

from ingredient import Ingredient
from accessToken import getAccessToken
from locationCall import location_ID

from typing import List, Optional, Dict, Any
from scaleIngredient import scaleIngredient


'''
Test Search Ingredients Functionality
'''

'''
Make sure you have the required libraries installed:
python -m pip install python-dotenv
'''

# Location ID for Oklahoma City, OK: 540FC253

def main():
    
    ingredients: List[Ingredient] = []
    ingredients = search_ingredients("orange", 10)
    ingredients2 = search_ingredients("apple", 10)
    print(ingredients[0])
    
    # Print ingredient descriptions
    print("Number of ingredients found: \n", len(ingredients))

    # Regular Values 
    print("Regular Value: ") 
    for i in range (len(ingredients)): 
        print("Ingredient ", i+1, ": ", ingredients[i].describe())
        print("\n")
    
    
    # Test of Scale Ingredient 
    print("Scaled valued: ") 
    for i in range (len(ingredients)): 
        ing = scaleIngredient(ingredients[i], 0.5)
        print("Ingredient ", i+1, ": ", ing.describe())
        print("\n")

if __name__ == "__main__":
    main()
