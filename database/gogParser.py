import jsonHandler as jh
import datetime
from database.userGenerator import gen_users

gog, res = jh.loadJSON("./datasets", "GOGdataset")

def parse_gog():
    """
    Parses 'GOGdataset' creating seperate files for games, users and reviews
    """
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

            if("release_date" in elem.keys()):
                tmp = elem["release_date"][:10]
                rel_date = str(int((datetime.datetime.strptime(tmp,"%Y-%m-%d") - epoch_time).total_seconds()))
            else:
                rel_date = "null"

            games.append({"gid": str(index), "name": gname, "release_date": rel_date,"tags": tags})

            for review in elem["reviews"]:
                uname = review["name"]
                if(uname not in unames):
                    unames.append(uname)

        jh.saveJSON("./gog", "gameDB", games)

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

    
    jh.saveJSON("./gog", "userDB", users)
    jh.saveJSON("./gog", "reviewDB", reviews)

