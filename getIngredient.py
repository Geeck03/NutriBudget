
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from ingredient import Ingredient
from accessToken import getAccessToken
#Gets a single ingredient from the Kroger API
# Then adds it to a data class Ingredient

#   'https://api.kroger.com/v1/products?filter.brand={{BRAND}}&filter.term={{TERM}}&filter.locationId={{LOCATION_ID}}' \
# -H 'Accept: application/json' \
#  -H 'Authorization: Bearer {{TOKEN}}'


def get_ingredient(name: str, location_ID: str) -> Ingredient:
    search_url = "https://api-ce.kroger.com/v1/products"
    # Use the certification envrionment 

    # A dictionary of parameters to filter the search results. Will be added to the URL as query parameters.
    # “Search for a product called banana, limit results to 1,
    #  and get data for store #0123456789.”
    params = {
        "filter.term": name,
        "filter.limit": 5,
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

    print(data)

    '''
    # Extract product info (first product)
    product = data["data"][0]
    item = product["items"][0]

    # Call USDA API for nutrition info here? 

    ingredient = Ingredient(
        name= name,
        price=item["price"]["regular"]
    )
   '''
    
    ingredient = Ingredient()
    ingredient.name = name
    ingredient.price = 0.0
     
    #return ingredient
    return ingredient 