import requests
import time
import json
import random
import jsonHandler as jh
from datetime import datetime
import string

DOMAIN = "http://localhost:8080/"

def get_mov_rnd_item(src_list, dst_list):
    rnd_item = random.choice(src_list)
    src_list.remove(rnd_item)
    dst_list.append(rnd_item)
    return rnd_item


def get_rnd_item(src_list):
    rnd_item = random.choice(src_list)
    return rnd_item


def login_request(uname, pwd):
    url = DOMAIN + 'login'
    data = {'uname': uname, 'pwd': pwd}
    response = requests.post(url, data=data)
    return response


def logout_request():
    url = DOMAIN  + 'logout' 
    response = requests.post(url)
    return response.json()


def post_new_review(db_data):
    rnd_user = get_rnd_item(db_data["users"])
    login_request(rnd_user["uname"], rnd_user["pwd"])

    new_review = get_mov_rnd_item(db_data["reviews"], db_data["reviews_sent"])

    rating = "on" if  new_review["rating"] else  "off"

    data_review = {
        "content" : new_review["content"],
        "rating" : rating
    }
    
    alphabet = string.ascii_uppercase
    random_letter = random.choice(alphabet)
    offset = random.randint(0,100)
    url = DOMAIN + f'search/game/name/{random_letter}?offset={offset}'
    response = requests.get(url)
    games_list = [json.loads(json_str) for json_str in response]
    if len(games_list) == 0:
        return

    url = DOMAIN + f'newReview/{games_list[0]["id"]}'
    response = requests.post(url, data=data_review)
    return response


def post_new_game(db_data):
    login_request("aaa", "wasd") #admin

    new_game = get_mov_rnd_item(db_data["games"], db_data["games_sent"])

    url = DOMAIN + 'newGame/'

    del new_game["gid"]

    datetime_object = datetime.fromtimestamp(new_game["release_date"])
    formatted_date = datetime_object.strftime('%Y-%m-%d')
    new_game["release_date"] = formatted_date

    formatted_tags = ", ".join(new_game["tags"])
    new_game["tags"] = formatted_tags

    response = requests.post(url, data=new_game)
    return response


def add_game_to_library(db_data):
    rnd_user = get_rnd_item(db_data["users"])
    login_request(rnd_user["uname"], rnd_user["pwd"])

    alphabet = string.ascii_uppercase
    random_letter = random.choice(alphabet)
    offset = random.randint(0,100)
    url = DOMAIN + f'search/game/name/{random_letter}?offset={offset}'
    response = requests.get(url)
    games_list = [json.loads(json_str) for json_str in response]
    if len(games_list) == 0:
        return

    url = DOMAIN + f'addGame/{games_list[0]["id"]}' 
    response = requests.get(url)
    return response


""" def upvote_review(db_data):
    rnd_user = get_rnd_item(db_data["users"])
    login_request(rnd_user["uname"], rnd_user["pwd"])

    review = get_rnd_item(db_data["reviews"])

    url = DOMAIN + f'upvoteReview/{review["rid"]}' 
    response = requests.get(url)
    return response.json() """


def update_played_hours(db_data):
    rnd_user = get_rnd_item(db_data["users"])
    login_request(rnd_user["uname"], rnd_user["pwd"])

    url = DOMAIN + f'search/games/{rnd_user}/more?offset=0&limit=100'
    response = requests.get(url)
    games_list = [json.loads(json_str) for json_str in response]
    if len(games_list) == 0:
        return

    game = get_rnd_item(games_list)
    hours = random.randint(1,10)

    url = DOMAIN + f'/updateHours/{game["id"]}'
    data = {'hours': hours}
    response = requests.post(url, data=data)
    return response.json()


def send_random_request(domain,requests, db_data):
    request_list, probabilities = zip(*requests)
    index = random.choices(range(len(request_list)), probabilities)[0]

    request_list[index](domain, db_data)
    logout_request()

if __name__ == "__main__":
    requests = [
        (post_new_game(),       0.05),
        (post_new_review(),     0.1),
        (add_game_to_library(), 0.3),
        (upvote_review(),       0.15),
        (update_played_hours(), 0.3),
        (send_random_request(), 0.1)
    ]

    users = jh.loadJSON("./final", "users")[0]
    games = jh.loadJSON("./final", "new_games")[0]
    reviews = jh.loadJSON("./final", "cur_reviews")[0]
    games_sent = []
    reviews_sent = []

    db_data = {
        "users": users,
        "games": games,
        "reviews": reviews,
        "games_sent": games_sent,
        "reviews_sent": reviews_sent
    }