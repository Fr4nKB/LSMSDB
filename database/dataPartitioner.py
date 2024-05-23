import jsonHandler as jh
import random

def split_games(split_ratio):
    games = jh.loadJSON("./final", "gameDB")[0]

    tot_games = len(games)
    tot_new_games = int(tot_games * split_ratio)
    
    random.shuffle(games)  # Shuffle the entries randomly

    new_games = games[:tot_new_games]
    old_games = games[tot_new_games:]

    jh.saveJSON("./final", "old_gameDB", old_games)
    jh.saveJSON("./final", "new_gameDB", new_games)


def split_reviews(split_ratio):
    reviews = jh.loadJSON("./final", "reviewDB_skimmed")[0]
    new_games = jh.loadJSON("./final", "new_gameDB")[0]

    games_dict = {dict_['gid']: dict_ for dict_ in new_games}

    new_reviews = []
    other_reviews = []
    for review in reviews:
        if review["gid"] in games_dict:
            new_reviews.append(review)
        else:
            other_reviews.append(review)

    jh.saveJSON("./final", "new_reviewDB", new_reviews)

    # free some memory
    new_reviews.clear()
    reviews.clear()
    new_games.clear()
    games_dict.clear()

    tot_reviews = len(other_reviews)
    tot_cur_reviews = int(tot_reviews * split_ratio)
    
    random.shuffle(other_reviews)  # Shuffle the entries randomly

    cur_reviews = other_reviews[:tot_cur_reviews]
    old_reviews = other_reviews[tot_cur_reviews:]

    jh.saveJSON("./final", "cur_reviewDB", cur_reviews)
    jh.saveJSON("./final", "old_reviewDB", old_reviews)

#split_games(0.1)
split_reviews(0.1)
