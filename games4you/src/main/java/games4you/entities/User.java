package games4you.entities;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import games4you.util.Authentication;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;

public class User {

    /**
     * Creates a new user
     * @param args firstname, lastname, datebirth, username, password, admin(0 or 1)
     * @return false if user wasn't created, true otherwise
     */
    public boolean signup(List<Object> args) {
        if (args.size() != 8) {
            return false;
        }

        MongoManager mongo = MongoManager.getInstance();

        int uid = (Integer) args.getFirst();
        String uname = (String) args.get(4);
        String pwd = (String) args.get(5);

        //check if username already in use
        if(mongo.findDocumentByKeyValue("users", "uname", uname).hasNext()) return false;

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(uname) && Authentication.isPassword(pwd))) return false;

        //check if user is banned
        Document user = new Document();
        user.append("firstname", args.get(1));
        user.append("lastname", args.get(2));
        user.append("datebirth", args.get(3));
        user.append("uname", uname);

        MongoCursor<Document> cur = mongo.findDocument("blacklist", user);
        if(cur.hasNext()) return false;

        //add remaining data
        user.append("uid", uid);
        user.append("pwd", Authentication.hashAndSalt(pwd, ""));
        user.append("isAdmin", args.get(6));

        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.addDoc("users", user);
        boolean ret = neo4j.addNode("User", uid);
        if(!ret) {
            mongo.removeDoc(false, "users", "uid", uid);
            return false;
        }
        else {
            neo4j.addAttribute("User", uid, "uname", uname);
            neo4j.addAttribute("User", uid, "tags", args.get(7));
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
            return user.getBoolean("isAdmin") ? 1 : 0;
        }
        else return -1;
    }

    public boolean publishReview(int uid, int gid, int rid) {
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


    /**
     * Creates a new user
     * @param args game_name, username, rating, creation_date, content, published, votes
     * @return -1 if the review couldn't be added, 0 if already present, 1 if added
     */
    public int addReview(List<Object> args) {

        if(args.size() != 11) return -1;

        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> reviews = mongo.getCollection("reviews");

        int rid = (Integer) args.getFirst();
        int gid = (Integer) args.get(1);
        int uid = (Integer) args.get(2);
        String game = (String) args.get(3);
        String uname = (String) args.get(4);
        String review_id = STR."\{game}|\{uname}";

        //check if review is already present
        Document existingReview = reviews.find(eq("review", review_id))
                .projection(fields(include("_id"), excludeId()))
                .first();
        if(existingReview != null) return 0;

        Neo4jManager neo4j = Neo4jManager.getInstance();

        Document review = new Document();
        review.append("rid", rid);
        review.append("gid", gid);
        review.append("uid", uid);
        review.append("game", game);
        review.append("uname", uname);
        review.append("rating", args.get(5));
        review.append("creation_date", args.get(6));
        review.append("content", args.get(7));
        review.append("reporters", args.getLast());

        boolean published = (Boolean) args.get(8);

        //if review is new it first needs to be checked by an admin
        if(!published) mongo.addDoc("uncheckedReviews", review);
        //otherwise it's normally added
        else {

            mongo.addDoc("reviews", review);

            neo4j.addNode("Review", rid);
            neo4j.addAttribute("Review", rid, "game", game);
            neo4j.addAttribute("Review", rid, "uname", uname);
            publishReview(uid, gid, rid);

            //set votes attribute, useful when populating the db
            int votes = (Integer) args.get(9);
            if(votes > 0) {
                String[] node_type = {"Game", "Review"};
                int[] node_name = {gid, rid};
                neo4j.incAttribute(node_type, node_name, "HAS_REVIEW", "votes", (Integer) args.get(9));
            }

        }

        return 1;
    }

    public boolean removeReview(int rid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.removeDoc(false, "reviews", "rid", rid);
        neo4j.removeNode("Review", rid);

        return true;
    }

}
