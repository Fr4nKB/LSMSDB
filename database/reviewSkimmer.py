import jsonHandler as jh

reviews = jh.loadJSON("./final", "reviewDB")[0]
print("B", len(reviews))
reviews = [entry for entry in reviews if len(entry['content']) >= 1400]
print("A", len(reviews))
reviews = jh.saveJSON("./final", "reviewDB_skimmed", reviews)[0]