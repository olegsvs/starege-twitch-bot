import requests
from dotenv import load_dotenv
import dotenv
import os

load_dotenv()
token_channel = os.getenv("CHANNEL_OAUTH_TOKEN")
refresh_token_channel = os.getenv("CHANNEL_REFRESH_TOKEN")
token_bot = os.getenv("SENTRY_OAUTH_TOKEN")
refresh_token_bot = os.getenv("SENTRY_REFRESH_TOKEN")
#refresh_token_test_channel =  os.getenv("CR")
url = "https://id.twitch.tv/oauth2/token"

payload='grant_type=refresh_token&refresh_token=' + refresh_token_channel + '&client_id=&client_secret='
headers = {
  'Content-Type': 'application/x-www-form-urlencoded'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
ACCESS_CHANNEL_TOKEN=response.json()['access_token']
REFRESH_CHANNEL_TOKEN=response.json()['refresh_token']
TIME_CHANNEL_TOKEN=response.json()['expires_in']
dotenv.set_key('.env', "CHANNEL_OAUTH_TOKEN", ACCESS_CHANNEL_TOKEN)
dotenv.set_key('.env', "CHANNEL_REFRESH_TOKEN", REFRESH_CHANNEL_TOKEN)
dotenv.set_key('.env', "CHANNEL_EXPIRES_IN", str(TIME_CHANNEL_TOKEN))



payload='grant_type=refresh_token&refresh_token=' + refresh_token_bot + '&client_id=&client_secret='
headers = {
  'Content-Type': 'application/x-www-form-urlencoded'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
ACCESS_BOT_TOKEN=response.json()['access_token']
REFRESH_BOT_TOKEN=response.json()['refresh_token']
TIME_BOT_TOKEN=response.json()['expires_in']
dotenv.set_key('.env', "SENTRY_OAUTH_TOKEN", ACCESS_BOT_TOKEN)
dotenv.set_key('.env', "SENTRY_REFRESH_TOKEN", REFRESH_BOT_TOKEN)
dotenv.set_key('.env', "SENTRY_EXPIRES_IN", str(TIME_BOT_TOKEN))
