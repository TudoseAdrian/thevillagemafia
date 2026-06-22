import json
import jwt
import requests
import time
import sys

def get_token():
    with open('service-account.json', 'r') as f:
        sa = json.load(f)

    iat = int(time.time())
    exp = iat + 3600
    payload = {
        'iss': sa['client_email'],
        'sub': sa['client_email'],
        'aud': 'https://oauth2.googleapis.com/token',
        'iat': iat,
        'exp': exp,
        'scope': 'https://www.googleapis.com/auth/firebase https://www.googleapis.com/auth/cloud-platform'
    }

    signed_jwt = jwt.encode(payload, sa['private_key'], algorithm='RS256')
    
    resp = requests.post('https://oauth2.googleapis.com/token', data={
        'grant_type': 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion': signed_jwt
    })
    
    if resp.status_code != 200:
        print("Error fetching token:", resp.text)
        sys.exit(1)
        
    print(resp.json()['access_token'])

if __name__ == '__main__':
    get_token()
