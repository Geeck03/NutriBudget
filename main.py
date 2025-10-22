
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from getIngredient import get_ingredient
from ingredient import Ingredient
from accessToken import getAccessToken
from locationCall import location_ID

# Your application is now granted access to the following APIs. When requesting an OAuth2 access token, 
# you must provide the scopes associated with the registered API you need to access.
#from data_handler import load_data


# Location ID for Oklahoma City, OK: 540FC253

def main():

    ingredient = Ingredient(); 

    # 01400943
    ingredient = get_ingredient("milk", "540FC253")
    ingredient.describe()

    print("Test! 8")
    #locationID = location_ID("73131") 
    #print("Location ID Data: ", locationID)
    #string = getAccessToken();
    

    #sample = get_ingredient("apple", "01400413")
    #sample.describe() # Print the ingredient details



if __name__ == "__main__":
    main()
