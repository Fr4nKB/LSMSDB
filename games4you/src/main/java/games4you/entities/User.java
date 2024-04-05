package games4you.entities;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
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

    /**
     * Creates a new user
     * @param args game_name, username, rating, creation_date, content, published, votes
     * @return -1 if the review couldn't be added, 0 if already present, 1 if added
     */
    public int addReview(HashMap<String, Object> args) {

        if(args.size() < 9) return -1;

        MongoManager mongo = MongoManager.getInstance();
        int rid = -1, gid = -1, uid = -1, votes = 0;
        String game = "", uname = "";
        Object reports = null;
        Document review = new Document();

        //check if all necessary fields are present
        try {
            rid = (Integer) args.get("rid");
            gid = (Integer) args.get("gid");
            uid = (Integer) args.get("uid");
            game = (String) args.get("game");
            uname = (String) args.get("uname");
            review.append("rating", (boolean) args.get("rating"));
            review.append("creation_date", (int) args.get("creation_date"));
            review.append("content", (String) args.get("content"));
            if(args.size() >= 10) votes = (int) args.get("votes");
            if(args.size() == 11) reports = args.get("reports");
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        //check if review is already present
        MongoCollection<Document> reviews = mongo.getCollection("reviews");
        Document existingReview = reviews.find(
                and(eq("gid", gid), eq("uid", uid)))
                .projection(fields(include("_id")))
                .first();
        if(existingReview != null) return 0;

        //add remaining fields and add document
        review.append("rid", rid);
        review.append("gid", gid);
        review.append("uid", uid);
        review.append("game", game);
        review.append("uname", uname);
        mongo.addDoc("reviews", review);

        Neo4jManager neo4j = Neo4jManager.getInstance();
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", rid);
        map.put("game", game);
        map.put("uname", uname);
        boolean ret = neo4j.addNode("Review", map);
        if(!ret) {
            mongo.removeDoc(false, "reviews", "rid", rid);
            return -1;
        }
        //create relationships between review, game and gamer
        publishReview(uid, gid, rid);

        //used to initially populate the db
        if(votes > 0) {
            String[] node_type = {"Game", "Review"};
            int[] node_name = {gid, rid};
            neo4j.incAttribute(node_type, node_name, "HAS_REVIEW", "votes", votes);
        }

        return 1;
    }


    private boolean publishReview(int uid, int gid, int rid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        long timestamp = Instant.now().getEpochSecond();
        String query = String.format(
                "MATCH (u:User {id: %d}), (g:Game {id: %d}), (r:Review {id: %d}) " +
                        "MERGE (u)-[:HAS_WROTE {in:%d}]->(r) " +
                        "MERGE (g)-[:HAS_REVIEW {votes:0}]->(r) " +
                        "MERGE (u)-[:OWNS {hours:0}]->(g)",
                uid, gid, rid, timestamp);
        return neo4j.executeWriteTransactionQuery(query);
    }

    public boolean removeReview(int rid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.removeDoc(false, "reviews", "rid", rid);
        neo4j.removeNode("Review", rid);

        return true;
    }

}
