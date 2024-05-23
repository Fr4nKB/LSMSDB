import jsonHandler as jh
import requests, time, os
import pandas as pd

# parameters to steam web api
URL_INFO = "https://store.steampowered.com/api/appdetails"
PARAMS = {
    'key': "866F617F60414FB5189CFC99C23BFBE2"
}

def try_request(url, params):
    tries = 0
    while(True):
        try:
            res = requests.get(url = url, params=params,
                headers={"User-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0"}).json()
            break
        except:
            tries += 1
            print("FAILED ", tries, " times")
            time.sleep(6 * tries)
    return res


def get_all_app_id():
    file_path = './datasets/STEAMdataset.csv'
    dataset = pd.read_csv(file_path)
    return dataset['app_id'].unique().tolist()


def parse_steam(games_list, step):
    """
    Parses a list of steam games, saves a backup every 'step' games parsed
    The backups can be collapsed into one file using 'collapse_backups' method
    :parameter games_list: list of steam app ids to parse
    :parameter step: number of games after which a backup is created
    """

    parsed_list = []
    partial_list = []
    current_gid = 0
    first_gid = 0

    if(step <= 0):
        step = 10
    elif(step > 1000):
        step = 1000

    for i in range(len(games_list)):
        appid = str(games_list[i])
        PARAMS["appids"] = appid
        res = try_request(URL_INFO, PARAMS)
        
        if(res is None):
            print("Res is None: ", appid)
            print("RES: ", res)
            continue

        if(type(res) != dict):
            print("Res is not a dict for: ", appid)
            print("RES: ", res)
            continue

        if(appid not in res.keys()):
            print("Appid not found for: ", appid)
            print("RES: ", res)
            continue

        game = res[appid]
        if game["success"] is False:
            print("Success FALSE: ", appid)
            continue

        if("data" not in game.keys()):
            print("Data not found for: ", appid)
            continue

        game = game["data"]
        if(game["type"] != "game"):
            continue
        if("genres" not in game.keys()):
            continue
        if("categories" not in game.keys()):
            continue
        if("detailed_description" not in game.keys()):
            continue
        if("about_the_game" not in game.keys()):
            continue
        if("short_description" not in game.keys()):
            continue
        if("header_image" not in game.keys()):
            continue
        if("release_date" not in game.keys()):
            continue
        if("date" not in game["release_date"].keys()):
            continue

        tags = [elem["description"] for elem in game["genres"]]
        tags.extend(elem["description"] for elem in game["categories"])
        release_date = game["release_date"]["date"]
        description = game["detailed_description"]
        about_game = game["about_the_game"]
        short_description = game["short_description"]
        image = game["header_image"]

        parsed_list.append({
            "gid"   : current_gid,
            "name"  : game["name"],
            "tag"   : tags,
            "steam-id" : appid,
            "release_date" : release_date,
            "description" : description,
            "about_the_game" : about_game,
            "short_description" : short_description,
            "header_image" : image
        })

        print("SUCCESS: ", game["name"])

        current_gid += 1
        
        if((current_gid - first_gid) % (step) == 0):
            partial_list = parsed_list[current_gid-step:]
            jh.saveJSON("./backups", f"games_fromID{current_gid - step}_toID{current_gid}", partial_list)
            
        time.sleep(1.5)

    jh.saveJSON("./steam", f"gameDB", parsed_list)

def check(name, gl):
    missing = []
    js, ret = jh.loadJSON("./steam", name)

    if(ret == False): return gl
    else:
        for elem in gl:
            found = False

            for game in js:
                if(int(game["steam-id"]) == elem):
                    found = True
                    break

            if(found == False):
                missing.append(elem)

    return missing

def collapse_backups():
    res = []
    for filename in os.listdir("./backups"):

        js, ret = jh.loadJSON("./backups", filename[:len(filename)-5])
        if(ret == False): continue
        for elem in js:
            res.append(elem)

    jh.saveJSON("./steam", "gameDB", res)
    
    missing = check("games", res)
    print("Missing: ", len(missing))

