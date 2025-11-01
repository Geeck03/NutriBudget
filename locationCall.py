
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from ingredient import Ingredient
from accessToken import getAccessToken

#
# https://api.kroger.com/v1/locations' \
 # -H 'Accept: application/json' \
  #-H 'Authorization: Bearer {{TOKEN}}'
# 


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
    
    return locationID.text; 

