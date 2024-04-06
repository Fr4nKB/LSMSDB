package games4you.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import games4you.util.Authentication;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;

public class User {

    /**
     * Creates a new user
     * @param args firstname, lastname, datebirth, username, password, admin(0 or 1)
     * @return false if user wasn't created, true otherwise
     */
    public boolean signup(HashMap<String, Object> args) {
        if (args.size() != 8) {
            return false;
        }

        MongoManager mongo = MongoManager.getInstance();

        //check if all necessary fields are present
        int uid = -1;
        String firstname = "", lastname = "", datebirth = "", uname = "", pwd = "";
        boolean isAdmin = false;
        ArrayList<String> tags;

        try {
            uid = (Integer) args.get("uid");
            firstname = (String) args.get("firstname");
            lastname = (String) args.get("lastname");
            datebirth = (String) args.get("datebirth");
            uname = (String) args.get("uname");
            pwd = (String) args.get("pwd");
            isAdmin = (boolean) args.get("isAdmin");
            tags = (ArrayList<String>) args.get("tags");
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Document user = new Document();
        user.append("firstname", firstname);
        user.append("lastname", lastname);
        user.append("datebirth", datebirth);
        user.append("uname", uname);

        //check if username already in use
        if(mongo.findDocumentByKeyValue("users", "uname", uname).hasNext()) return false;

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(uname) && Authentication.isPassword(pwd))) return false;

        //check if user is banned
        MongoCursor<Document> cur = mongo.findDocument("blacklist", user);
        if(cur.hasNext()) return false;

        //add remaining data
        user.append("uid", uid);
        user.append("pwd", Authentication.hashAndSalt(pwd, ""));
        user.append("isAdmin", isAdmin);

        mongo.addDoc("users", user);

        Neo4jManager neo4j = Neo4jManager.getInstance();

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", uid);
        map.put("uname", uname);
        boolean ret = neo4j.addNode("User", map);

        if(!ret) {
            mongo.removeDoc(false, "users", "uid", uid);
            return false;
        }
        else neo4j.addAttribute("Review", uid, "tags", tags);
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
            return user.getBoolean("isAdmin") ? 1 : 0;
        }
        else return -1;
    }

    public boolean removeReview(int rid) {
        Review review = new Review();
        return review.removeReview(rid);
    }

    public String showGame(int gid){
        MongoManager mongo = MongoManager.getInstance();

        MongoCollection<Document> games = mongo.getCollection("games");
        Document doc = games.find(
                        Filters.eq("gid", gid))
                .projection(Projections.excludeId())
                .first();

        if(doc == null){
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try{
            return mapper.writeValueAsString(doc);
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


}
