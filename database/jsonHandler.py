import os, json

jsonFile = {}

#name of the json file to load in jsonFile (ext. excluded)
def loadJSON(dir, name):
    global jsonFile

    if not os.path.exists(dir): 
        os.makedirs(dir) 

    try:
        file = open(dir+"/"+name+".json", "r", encoding="utf-8")
        jsonFile = json.load(file)
        file.close()
        return jsonFile, True
    except:
        print("Missing "+name+".json")
        return {}, False

def saveJSON(dir, name, contents):
    global jsonFile

    if not os.path.exists(dir): 
        os.makedirs(dir) 

    try:
        file = open(dir+"/"+name+".json", "w")
        json.dump(contents, file, indent=2)
        file.close()
        return {}, True
    except:
        print("Some error occurred while saving "+name+".json")
        return {}, False
    