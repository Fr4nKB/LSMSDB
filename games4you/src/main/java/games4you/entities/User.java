package games4you.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import games4you.util.Authentication;
import games4you.util.Constants;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class User {

    private ArrayList<Object> getRelationshipList(String query) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    private ArrayList<Object> getReviewList(String node_type, long id, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String relation;
        if(Objects.equals(node_type, "Game")) relation = "HAS_REVIEW";
        else if(Objects.equals(node_type, "User")) relation = "HAS_WROTE";
        else return null;

        String query = String.format("""
                        MATCH (:%s {id: %d})-[:%s]->(b:Review)
                        RETURN {rid: b.id, gid: b.gid, uid: b.uid, game: b.game, uname: b.uname, rating: b.rating} AS result
                        ORDER BY b.creation SKIP %d LIMIT %d""",
                node_type, id, relation, offset, limit);
        return getRelationshipList(query);
    }

    /**
     * Creates a new user
     * @param args firstname, lastname, datebirth, username, password, admin(0 or 1)
     * @return false if user wasn't created, true otherwise
     */
    public boolean signup(HashMap<String, Object> args) {
        if (args.size() != 8) return false;

        MongoManager mongo = MongoManager.getInstance();

        //check if all necessary fields are present
        long uid, datecreation;
        String firstname, lastname, datebirth, uname, pwd;
        boolean isAdmin;

        try {
            uid = (Long) args.get("uid");
            firstname = (String) args.get("firstname");
            lastname = (String) args.get("lastname");
            datebirth = (String) args.get("datebirth");
            uname = (String) args.get("username");
            pwd = (String) args.get("pwd");
            isAdmin = (boolean) args.get("isAdmin");
            datecreation = (Long) args.get("datecreation");
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
        if(!(Authentication.isName(uname) && Authentication.isPassword(pwd))) return false;

        //check if user is banned
        MongoCursor<Document> cur = mongo.findDocument("blacklist", user);
        if(cur.hasNext()) return false;

        //add remaining data
        user.append("uid", uid);
        user.append("pwd", Authentication.hashAndSalt(pwd));
        user.append("isAdmin", isAdmin);
        user.append("datecreation", datecreation);

        if(!mongo.addDoc("users", user)) return false;

        if(isAdmin) return true;

        Neo4jManager neo4j = Neo4jManager.getInstance();

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", uid);
        map.put("uname", uname);
        boolean ret = neo4j.addNode("User", map);
        if(!ret) {
            mongo.removeDoc(false, "users", "uid", uid);
            return false;
        }
        else return true;
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
        if(!(Authentication.isName(uname) && Authentication.isPassword(pwd))) return null;

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

    public ArrayList<Object> getFriendList(long uid, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String query = String.format("""
                        MATCH (:User {id: %d})-[r:IS_FRIEND_WITH]-(b:User)
                        RETURN {type: \"U\", id: b.id, name: b.uname, since: r.since} AS result
                        SKIP %d LIMIT %d""",
                uid, offset, limit);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getGameList(long uid, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String query = String.format("""
                        MATCH (:User {id: %d})-[r:OWNS]->(b:Game)
                        RETURN {type: \"G\", id: b.id, name: b.name, hours: r.hours} AS result
                        SKIP %d LIMIT %d""",
                uid, offset, limit);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getGameReviewList(long gid, int offset) {
        return getReviewList("Game", gid, offset, Constants.getDefPagLim());
    }

    public ArrayList<Object> getUserReviewList(long uid, int offset) {
        return getReviewList("User", uid, offset, Constants.getDefPagLim());
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

    public String showReview(long rid){
        MongoManager mongo = MongoManager.getInstance();

        MongoCollection<Document> reviews = mongo.getCollection("reviews");
        Document doc = reviews.find(
                        Filters.eq("rid", rid))
                .projection(Projections.exclude("_id"))
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


    public String showUser(long uid){
        MongoManager mongo = MongoManager.getInstance();

        MongoCollection<Document> users = mongo.getCollection("users");
        Document doc = users.find(
                Filters.eq("uid", uid))
                .projection(Projections.exclude("_id", "pwd", "isAdmin"))
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


    public ArrayList<Object> browseUsers(String username, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String query = String.format("""
                        MATCH (u:User) WHERE ToLower(u.uname) CONTAINS ToLower('%s')
                        RETURN {type: \"U\", id: u.id, name: u.uname}
                        SKIP %d LIMIT %d""",
                username, offset, limit);

        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    private ArrayList<Object> browseGames(String key, String key_value, int offset, int limit){
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String query;
        if(key.equals("name")) {
            query = String.format("""
                            MATCH (g:Game) WHERE ToLower(g.name) CONTAINS ToLower("%s")
                            RETURN {type: \"G\", id: g.id, name: g.name}
                            SKIP %d LIMIT %d""",
                    key_value, offset, limit);
        }
        else if(key.equals("tags")) {
            query = String.format("""
                            MATCH (g:Game)
                                 WHERE ANY(tag IN g.tags WHERE ToLower(tag) CONTAINS ToLower("%s"))
                                 RETURN {type: \"G\", id: g.id, name: g.name, tags: g.tags}
                                 SKIP %d LIMIT %d""",
                    key_value, offset, limit);
        }
        else return null;

        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> browseGamesByName(String name, int offset, int limit) {
        return browseGames("name", name, offset, limit);
    }

    public ArrayList<Object> browseGamesByTags(String tag, int offset, int limit) {
        return browseGames("tags", tag, offset, limit);
    }

}
