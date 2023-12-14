import jsonHandler as jh
import requests, time, datetime, sys

gog, res = jh.loadJSON("./datasets", "GOGdataset")

def gen_users(numUsers, users = False):
    URL = "https://randomuser.me/api/"
    toSave = []

    uid = 0
    rem = numUsers
    while(rem > 0):
        if(rem > 5000):
            batch = 5000
        else:
            batch = rem
        PARAMS = {
            'results': batch,
            'inc': 'name, login, dob, registered'
        }

        min = 1
        while(True):
            try:
                res = requests.get(url = URL, params=PARAMS,
                           headers={"User-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0"}).json()["results"]
                break
            except:
                #wait to avoid overload of requests
                print("waiting for", min, "minutes")
                time.sleep(min*60)
                min += 1

        for elem in res:
            if(users):
                username = users[uid]
            else:
                username = elem["login"]["username"]
            
            toSave.append({"uid": str(uid), "firstname": elem["name"]["first"], "lastname": elem["name"]["last"],
                "datebirth": elem["dob"]["date"], "pwd": elem["login"]["password"], "username": username,
                "datecreation": elem["registered"]["date"]})
            uid += 1
            
        rem -= batch
        print(numUsers-rem,"/",numUsers)
            
    return toSave

def parse_gog():

    gnames = []
    games = []
    unames = []
    users = []
    reviews = []
    epoch_time = datetime.datetime(1970, 1, 1)

    if(res):
        for index, elem in enumerate(gog):
            gname = elem["name"]
            if(gname not in gnames):
                    gnames.append(gname)

            tags = elem["genres"]
            if(elem["single_player"]):
                tags.append("Single-player")
            if(elem["multi_player"]):
                tags.append("Multi-player")

            games.append({"gid": str(index), "gamename": gname, "tags": tags})

            for review in elem["reviews"]:
                uname = review["name"]
                if(uname not in unames):
                    unames.append(uname)

        rid = 0
        for elem in gog:
            gid = gnames.index(elem["name"])
            for review in elem["reviews"]:
                uid = unames.index(review["name"])
                
                #rating normalization
                if(review["rating"] >= 3):
                    rating = True
                else:
                    rating = False

                creation_date = str(int((datetime.datetime.strptime(review["creation_date"],"%B %d, %Y") - epoch_time).total_seconds()))
                
                reviews.append({"rid": rid, "gid": gid, "uid": uid, "rating": rating, "creation_date": creation_date,
                                "content": review["content"], "published": True})
                rid += 1

        users = gen_users(len(unames), unames)

    jh.saveJSON("./gog", "gameDB", games)
    jh.saveJSON("./gog", "userDB", users)
    jh.saveJSON("./gog", "reviewDB", reviews)

parse_gog()