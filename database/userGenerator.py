import requests, time

def gen_users(num_users, users = False, start_uid = False):
    """
    Generates users by scraping randomuser.me
    :parameter num_users: number of users to generate
    :parameter users: (optional) list of usernames to override randomuser.me usernames
    :parameter start_uid: (optional) first user id, next ones will increment by one from there
    :return: a list of dicts containg num_users users
    """

    URL = "https://randomuser.me/api/"
    toSave = []

    if(start_uid):
        uid = start_uid
    else:
        uid = 0
        
    rem = num_users
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
        print(num_users-rem, "/", num_users)
            
    return toSave