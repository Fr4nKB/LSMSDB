package games4you;

import com.mongodb.client.MongoCursor;
import games4you.manager.MongoManager;
import games4you.manager.Neo4jManager;
import games4you.util.Authentication;
import org.bson.Document;

public class User {

    /**
     * Creates a new user
     * @param args firstname, lastname, datebirth, username, password, admin(0 or 1)
     * @return false if user wasn't created, true otherwise
     */
    public boolean signup(String [] args) {
        if (args.length != 6) {
            return false;
        }

        MongoManager mongo = MongoManager.getInstance();

        //check if username already present
        if (mongo.findDocumentByKeyValue("users", "uname", args[3]).hasNext()) return false;

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(args[3]) && Authentication.isPassword(args[4]))) return false;

        Neo4jManager neo4j = Neo4jManager.getInstance();

        //Creates document with all the data of the user
        Document user = new Document();
        user.append("firstname", args[0]);
        user.append("lastname", args[1]);
        user.append("datebirth", args[2]);
        user.append("uname", args[3]);
        user.append("pwd", Authentication.hashAndSalt(args[4], ""));
        user.append("isAdmin", args[5]);

        mongo.addElem("users", user);
        boolean ret = neo4j.addUser(args[3]);
        if(!ret) {
            mongo.removeElem("users", "uname", args[0]);
            return false;
        }
        return true;
    }

    /**
     * Checks authentication data
     * @param uname username
     * @param pwd password
     * @return -1 if data is wrong, 0 if user is normal and 1 if admin
    */
    public int login(String uname, String pwd) {
        MongoManager mongo = MongoManager.getInstance();

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(uname) && Authentication.isPassword(pwd))) return -1;

        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("users", "uname", uname);
        if(cur.hasNext()) {
            Document user = cur.next();
            if(!Authentication.verifyHash(user.getString("pwd"), pwd)) return -1;
            return Integer.parseInt(user.getString("isAdmin"));
        }
        else return -1;
    }

}
