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

import java.util.ArrayList;
import java.util.HashMap;

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
        long uid;
        String firstname, lastname, datebirth, uname, pwd;
        boolean isAdmin;
        ArrayList<String> tags;

        try {
            uid = (Long) args.get("uid");
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
        user.append("pwd", Authentication.hashAndSalt(pwd));
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
    public long[] login(String uname, String pwd) {
        MongoManager mongo = MongoManager.getInstance();

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(uname) && Authentication.isPassword(pwd))) return null;

        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("users", "uname", uname);
        if(cur.hasNext()) {
            Document user = cur.next();
            if(!Authentication.verifyHash(user.getString("pwd"), pwd)) return null;

            long[] ret = new long[2];
            ret[0] = user.getLong("uid");
            ret[1] = user.getBoolean("isAdmin") ? 1 : 0;
            return ret;
        }
        else return null;
    }

    public boolean removeReview(long rid) {
        Review review = new Review();
        return review.removeReview(rid);
    }

    public String showGame(long gid){
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

    public String showUser(int uid){
        MongoManager mongo = MongoManager.getInstance();

        MongoCollection<Document> users = mongo.getCollection("users");
        Document doc = users.find(
                Filters.eq("uid", uid))
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


    public ArrayList<Object> browseUsers(String username, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String query = String.format(
                "MATCH (u:User) WHERE ToLower(u.uname) CONTAINS ToLower('%s') " +
                        "RETURN {type: \"U\", id: u.id, name: u.uname} " +
                        "SKIP %d LIMIT 20",
                username, offset);
        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> browseGames(String gameName, int offset){
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String query = String.format(
                "MATCH (g:Game) WHERE ToLower(g.name) CONTAINS ToLower('%s') " +
                        "RETURN {type: \"G\", id: g.id, name: g.name} " +
                        "SKIP %d LIMIT 20",
                gameName, offset);
        return neo4j.getQueryResultAsList(query);
    }

}
