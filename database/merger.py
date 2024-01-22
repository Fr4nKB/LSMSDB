import jsonHandler as jh
import datetime
import pandas as pd
import random

epoch_time = datetime.datetime(1970, 1, 1)

def merge_games():
    """
    Merges gog and steam databases, the first steam game will have a gid equal to the latest gid used in gog +1
    Release date is reformatted in seconds from EPOCH and redundant data is eliminated
    """
    gog = jh.loadJSON("./gog", "gameDB")[0]
    steam = jh.loadJSON("./steam", "gameDB")[0]

    last_gid = int(gog[len(gog)-1]["gid"]) + 1

    for index, elem in enumerate(steam):
        elem["gid"] = str(last_gid + index)
        try:
            rel_date = str(int((datetime.datetime.strptime(elem["release_date"], "%d %b, %Y") - epoch_time).total_seconds()))
            elem["release_date"] = rel_date
        except:
            del elem["release_date"]
        del elem["about_the_game"]
        gog.append(elem)

    jh.saveJSON("./final", "gameDB", gog)

def create_and_merge_steam_reviews():
    """
    Generates steam reviews by parsing the kaggle database, converts from steam ids to gids
    and merges everything in reviewDB.json
    """

    game = jh.loadJSON("./final", "gameDB")[0]
    users = jh.loadJSON("./final", "userDB")[0]
    review = jh.loadJSON("./gog", "reviewDB")[0]
    dataset = pd.read_csv("./datasets/STEAMdataset.csv").to_dict(orient='records')

    # create hash map steam-id -> gid
    hashMap = {}
    for elem in game:
        try:
            hashMap[str(elem["steam-id"])] = elem["gid"]
        except:
            continue

    new_reviews = []
    user_already_used = []
    starting_rid = len(review)
    numUsers = len(users)
    last_app_id = 0

    for rid, elem in enumerate(dataset):

        if elem["app_id"] != last_app_id:
            user_already_used.clear()

        while True:
            uid = random.randint(0, numUsers)
            if uid in user_already_used:
                continue
            else:
                user_already_used.append(uid)
                break

        if elem["review_score"] == 1:
            rating = True
        else:
            rating = False

        try:
            new_reviews.append({"rid": starting_rid + rid, "gid": hashMap[str(elem["app_id"])], "uid": uid,
                                "rating": rating, "content": elem["review_text"], "review_votes": elem["review_votes"],
                                "published": True})
        except:
            continue
        

    review.append(new_reviews)
        
    jh.saveJSON("./final", "reviewDB", review)
