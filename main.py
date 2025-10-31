
import requests # Library for making HTTP requests
import base64 # Library for encoding/decoding data in Base64 format

from getIngredient import get_ingredient
from getIngredient import get_price
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
    ingredient = get_ingredient("apple", "03400397")
    ingredient.describe()

    nameThing = ingredient.name
    price = ingredient.local_regular 

    local_reg = ingredient.local_regular
    local_reg_esitmate = ingredient.local_regular_per_unit_estimate
    local_promo = ingredient.local_promo,
    local_promo_per_unit_esimate = ingredient.local_promo_per_unit_estimate
    national_reg = ingredient.national_regular
    national_promo = ingredient.national_promo
    national_promo_per_unit_estimate= ingredient.national_promo_per_unit_estimate
    national_regular_per_unit_estimate= ingredient.national_regular_per_unit_estimate

    #locationID = location_ID("73131") 
    #print("Location ID Data: ", locationID) 
    #string = getAccessToken();
    
    print(nameThing)
    catTwo = ingredient.product_ID
    print(catTwo)
    #sample = get_ingredient("apple", "01400413")
    #sample.describe() # Print the ingredient details
    #ingredient = get_price(ingredient)
    
    print("Test! 6 \n")
    print("Local Regular Price: ", local_reg)
    print("Local Regular Per Unit Estimate: ", local_reg_esitmate)
    print("Local Promo Price: ", local_promo)
    print("Local Promo Per Unit Estimate: ", local_promo_per_unit_esimate)
    print("National Regular Price: ", national_reg)
    print("National Promo Price: ", national_promo) 
    print("National Promo Per Unit Estimate: ", national_promo_per_unit_estimate)
    print("National Regular Per Unit Estimate: ", national_regular_per_unit_estimate)   

    # 73131
    # 77407
    #locationID = location_ID("77407") 
    # print("Location ID Data: ", locationID)



if __name__ == "__main__":
    main()
