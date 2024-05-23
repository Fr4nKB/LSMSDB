import jsonHandler as jh
import uuid
import datetime
import time
from dateutil.parser import parse
import math
import random
from datetime import datetime, timedelta
import re
import string

def check_users():
    users = jh.loadJSON("./final_old", "userDB")[0]
    usernames = []

    for entry in users:

        if "username" not in entry:
            print("username missing")
            continue

        while entry['username'] in usernames:
            entry['username'] = entry['username'] + '1'
        usernames.append(entry['username'])

    jh.saveJSON("./final", "userDB", users)


def merge_dicts(entry1, entry2):
    merged_entry = entry1.copy()  # Start with a copy of the first dictionary

    # Iterate over the second dictionary
    for key, value in entry2.items():
        # If the key is not present in the first dictionary, add it
        if key not in merged_entry:
            merged_entry[key] = value

    return merged_entry


def update_reviews(game1, game2, reviews):
    for review in reviews:
        if review["gid"] == game2["gid"]:
            review["gid"] = game1["gid"]


def merge_duplicate_games():
    merged_entries = {}
    games = jh.loadJSON("./final_old", "gameDB")[0]
    reviews = jh.loadJSON("./final_old", "reviewDB")[0]

    for game in games:
        game_name = game.get('name')
        if game_name in merged_entries:
            merged_entries[game_name] = merge_dicts(merged_entries[game_name], game)
            update_reviews(merged_entries[game_name], game, reviews)
        else:
            merged_entries[game_name] = game
    
    jh.saveJSON("./final", "games_mergedDB", list(merged_entries.values()))
    jh.saveJSON("./final", "reviews_mergedDB", reviews)

def skimFields():
    users = jh.loadJSON("./final", "userDB")[0]
    games = jh.loadJSON("./final", "gameDB")[0]
    reviews = jh.loadJSON("./final", "reviewDB")[0]

    for entry in games:
        if('short_description' in entry):
            del entry['short_description']
        
        if('steam-id' in entry):
            del entry['steam-id']

    jh.saveJSON("./final", "gameDB_skimmed", games)

    games_dict = {dict_['gid']: dict_ for dict_ in games}

    for entry in reviews:
        if('review_votes' in entry):
            del entry['review_votes']
        
        if('published' in entry):
            del entry['published']

        entry['username'] = users[entry['uid']]['username']
        entry['game'] = games_dict[entry['gid']]['name']

    jh.saveJSON("./final", "reviewDB_skimmed", reviews)


def fixUsers():
    users = jh.loadJSON("./final", "userDB")[0]

    for entry in users:
        date_obj = parse(entry['datebirth'])
        entry['datebirth'] = date_obj.strftime('%d/%m/%Y')
        entry['datecreation'] = int(datetime.datetime.strptime(entry['datecreation'], "%Y-%m-%dT%H:%M:%S.%fZ").timestamp())
        entry['isAdmin'] = False
        if(type(entry['uid']) == str):
                entry['uid'] = int(entry['uid'])

    jh.saveJSON("./final", "userDB", users)


def fixGames():
    games = jh.loadJSON("./final_old", "gameDB")[0]

    for entry in games:
        if('release_date' not in entry):
            entry['release_date'] = int(time.time())
        if(type(entry['release_date']) == str):
            if(entry['release_date'] == "null"):
                entry['release_date'] = int(time.time())
            else:
                entry['release_date'] = int(entry['release_date'])
        
        if(type(entry['gid']) == str):
                entry['gid'] = int(entry['gid'])

    jh.saveJSON("./final", "gameDB", games)

def random_date():
    start = datetime.now() - timedelta(days=10*365)
    end = datetime.now()
    random_datetime = start + timedelta(
        seconds=random.randint(0, int((end - start).total_seconds())))
    return int(random_datetime.timestamp())

def fixReviews():
    rev = jh.loadJSON("./final", "reviewDB")[0]

    for entry in rev:
        if(type(entry['content']) != str and math.isnan(entry['content'])):
            entry['content'] = ""

        if('creation_date' not in entry):
            entry['creation_date'] = random_date()
        else:
            entry['creation_date'] = int(entry['creation_date'])

        if(type(entry['gid']) == str):
            entry['gid'] = int(entry['gid'])

    jh.saveJSON("./final", "reviewDB", rev)

def replace_uuid():
    translations = {}
    games = jh.loadJSON("./final_old", "gameDB")[0]
    users = jh.loadJSON("./final_old", "userDB")[0]
    reviews = jh.loadJSON("./final_old", "reviewDB")[0]

    for game in games:
        #uuid.uuid4().hex
        new_uuid = int(uuid.uuid4())
        translations[game["gid"]] = new_uuid
        game["gid"] = new_uuid

    for user in users:
        #uuid.uuid4().hex
        new_uuid = int(uuid.uuid4())
        translations[user["uid"]] = new_uuid
        user["uid"] = new_uuid

    for review in reviews:
        new_uid = translations[review["uid"]]
        new_gid = translations[review["gid"]]
        review["uid"] = new_uid
        review["gid"] = new_gid


def skimUsers():
    users = jh.loadJSON("./final", "userDB")[0]
    reviews = jh.loadJSON("./final", "reviewDB_skimmed")[0]

    users_dict = {dict_['uid']: dict_ for dict_ in users}
    users_dict_fin = {}
    
    for review in reviews:
        if review['uid'] not in users_dict_fin:
            users_dict_fin[review['uid']] = users_dict[review['uid']]

    jh.saveJSON("./final", "userDB_skimmed", list(users_dict_fin.values()))
    print(len(users_dict_fin))

def generate_random_string(length):
    letters_and_digits = string.ascii_letters + string.digits
    result_str = ''.join(random.choice(letters_and_digits) for _ in range(length))
    return result_str


def fixUsernames():
    users = jh.loadJSON("./final", "userDB_skimmed")[0]
    reviews = jh.loadJSON("./final", "reviewDB_skimmed")[0]

    users_dict = {dict_['uid']: dict_ for dict_ in users}

    for user in users:
        if(not bool(re.match("[a-zA-Z0-9_+.-]*$", user['username']))):
            user['username'] = generate_random_string(12)

    users_dict = {dict_['uid']: dict_ for dict_ in users}

    for entry in reviews:
        entry['username'] = users_dict[entry['uid']]['username']

    jh.saveJSON("./final", "userDB_skimmed1", list(users_dict.values()))
    jh.saveJSON("./final", "reviewDB_skimmed1", reviews)

fixUsernames()