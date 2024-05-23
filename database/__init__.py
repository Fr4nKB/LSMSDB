from database.gogParser import parse_gog
from steamParser import get_all_app_id, parse_steam
from database.merger import merge_games, create_and_merge_steam_reviews
from database.userGenerator import gen_users
import jsonHandler as jh

# parse gog and steam games, then merge into gameDB.json
parse_gog()
parse_steam(get_all_app_id(), 1000)
merge_games()

users = jh.loadJSON("./gog", "userDB")[0]
n_users = len(users)
# generate new users to reach 100k otherwise just save the actual users to final
if n_users < 100000:
    users.append(gen_users(100000 - n_users, False, n_users))

jh.saveJSON("./final", "userDB", users)

# finally create steam reviews and merge them to gog ones
create_and_merge_steam_reviews()
