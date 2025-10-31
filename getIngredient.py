
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format
import json

from ingredient import Ingredient
from accessToken import getAccessToken
#Gets a single ingredient from the Kroger API
# Then adds it to a data class Ingredient

# 'https://api.kroger.com/v1/products?filter.brand={{BRAND}}&filter.term={{TERM}}&filter.locationId={{LOCATION_ID}}' \
# -H 'Accept: application/json' \
#  -H 'Authorization: Bearer {{TOKEN}}'


# Separate concerns 
# Create a search function for ingredients 
# Add each ingredient to an Ingredient data class
# Return the Ingredient data classes to the UI 


def get_ingredient(name: str, location_ID: str) -> Ingredient:
    search_url = "https://api.kroger.com/v1/products"
    # Use the certification envrionment 

    # A dictionary of parameters to filter the search results. Will be added to the URL as query parameters.
    # “Search for a product called banana, limit results to 1,
    #  and get data for store #0123456789.”
    params = {
        "filter.term": name,
        "filter.limit": 1,
        "filter.locationId": location_ID # Example location ID
    } 
    # Set up the headers for the request, including the authorization token.

    # Function from get.py 
    token: str = ""
    token = getAccessToken()
    
    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}"
    }

    response = requests.get(search_url, headers=headers, params=params)

    # .json() method to parse the JSON response into a Python dictionary.
    # ["data"] accesses the "data" key from the JSON.
    # (Kroger’s API wraps product results in a "data" array.)
    # [0] picks the first product returned.

    if response.status_code != 200:
        print("Error", response.status_code, response.text)
        return Ingredient()  # Return an empty Ingredient on error 
    
    data = response.json()  # Get list of json responses
    # print("Data from get_ingredient \n")
    print(data)

    # print(data)

    
    
    # Extract product info (first product)
    product = data["data"][0]
    product_id = product["productId"]
    items = product["items"][0]


    # Get Nutrients Info
    # Gets JSON of nutrition info
    nutrition = product["nutritionInformation"][0]
    print("Nutrition Info: ", nutrition)


    # Get Regular Prices 
    local_regular = items.get('price', {}).get('regular', None)  # Regular price or None if missing
    regularPerUnitEstimate = items.get('price', {}).get('regularPerUnitEstimate', None)
    promo = items.get('price', {}).get('promo', None)
    promoPerUnitEstimate= items.get('price', {}).get('promoPerUnitEstimate', None)

    # Get National Prices 
    national_regular = items.get('nationalPrice', {}).get('regular', None)
    national_promo =  items.get('nationalPrice', {}).get('promo', None)
    national_promo_per_unit_estimate = items.get('nationalPrice', {}).get('promoPerUnitEstimate', None)
    national_regular_per_unit_estimate = items.get('nationalPrice', {}).get('regularPerUnitEstimate', None)


    print("Product ID: ", product_id)
    print("Price: ", local_regular)
    #price_info = product["items"]["price"]

    # Call USDA API for nutrition info here? 

    ingredient = Ingredient(
        name=name,
        product_ID=product_id,


        local_regular=local_regular,
        local_regular_per_unit_estimate=regularPerUnitEstimate,
        local_promo=promo,
        local_promo_per_unit_estimate=promoPerUnitEstimate,
        national_regular=national_regular,
        national_promo=national_promo,
        national_promo_per_unit_estimate=national_promo_per_unit_estimate,
        national_regular_per_unit_estimate=national_regular_per_unit_estimate
    )

    
    # Add me! 
    # Functionality for nutrition info can be added here
    #Get info serving size 
     
    #return ingredient
    return ingredient 


def get_price(ingredient: Ingredient) -> Ingredient:

    ID = ingredient.product_ID
    print(ID)
    search_url = f"https://api.kroger.com/v1/products/{ID}" 

    
    token = getAccessToken()
    
    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}"
    }

    params = {
        "filter.locationId": "540FC253" # Example location ID
    }


    response = requests.get(search_url, headers=headers, params=params)
    
    if response.status_code != 200:
        print("Error", response.status_code, response.text)
        return Ingredient()  # Return an empty Ingredient on error 
    
    data = response.json()  # Get list of json responses
    # print("Data from get_price \n")
    # print(data)

    return Ingredient() 