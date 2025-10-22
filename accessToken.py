
import requests
import base64 #  ? What is this 
# Get/V1/Products
# Put token here 
from dotenv import load_dotenv
import os

''''
curl -X POST \
  'https://api.kroger.com/v1/connect/oauth2/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Authorization: Basic {{base64(CLIENT_ID:CLIENT_SECRET)}}' \
  -d 'grant_type=client_credentials'
'''
def getAccessToken():
    load_dotenv()  # Load environment variables from .env file
    CLIENT_ID = os.getenv("CLIENT_ID")
    CLIENT_SECRET = os.getenv("CLIENT_SECRET")





    '''
    curl -X POST \
  'https://api.kroger.com/v1/connect/oauth2/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Authorization: Basic {{base64(CLIENT_ID:CLIENT_SECRET)}}' \
  -d 'grant_type=client_credentials'



    '''


    auth = base64.b64encode(f"{CLIENT_ID}:{CLIENT_SECRET}".encode()).decode()
    token_url = "https://api-ce.kroger.com/v1/connect/oauth2/token"

    data = {"grant_type": "client_credentials","scope": "product.compact"}

    headers = {"Content-Type": "application/x-www-form-urlencoded","Authorization": f"Basic {auth}"}


    response = requests.post(token_url, headers=headers, data=data)

    #print(response.status_code)
    #print(response.json())

  
    if response.status_code == 200: 
        print("Success")
        token = response.json()
        access_token = token.get("access_token")
        #print("Access Token:", access_token)
    if response.status_code != 200:
        print("Error", response.status_code, response.text)
        
  
    
    access_token = response.json().get("access_token")
    #print("Access Token:", access_token)

    return access_token 



