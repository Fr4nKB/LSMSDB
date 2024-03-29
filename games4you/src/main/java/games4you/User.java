package games4you;

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
    public boolean signup(List<String> args) {
        if (args.size() != 6) {
            return false;
        }

        MongoManager mongo = MongoManager.getInstance();

        //check if username already present
        if(mongo.findDocumentByKeyValue("users", "uname", args.get(3)).hasNext()) return false;

        //check if username and password contain allowed characters
        if(!(Authentication.isUsername(args.get(3)) && Authentication.isPassword(args.get(4)))) return false;

        Neo4jManager neo4j = Neo4jManager.getInstance();

        //Creates document with all the data of the user
        Document user = new Document();
        user.append("firstname", args.getFirst());
        user.append("lastname", args.get(1));
        user.append("datebirth", args.get(2));
        user.append("uname", args.get(3));
        user.append("pwd", Authentication.hashAndSalt(args.get(4), ""));
        user.append("isAdmin", args.get(5));

        mongo.addDoc("users", user);
        boolean ret = neo4j.addNode("User", args.get(3));
        if(!ret) {
            mongo.removeDoc("users", "uname", args.get(3));
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

    public boolean addFriend(String user1, String user2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        return neo4j.addRelationship(node_types, STR."IS_FRIEND_WITH {since:\{timestamp}}", user1, user2);
    }

    public boolean removeFriend(String user1, String user2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        return neo4j.removeRelationship(node_types, "IS_FRIEND_WITH", user1, user2);
    }

    public boolean addGameToOwned(String user, String game) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "Game"};
        return neo4j.addRelationship(node_types, "OWNS  {hours: 0}", user, game);
    }

    public boolean addReviewToPublished(String user, String game, String review) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "Review"};
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        boolean ret = neo4j.addRelationship(node_types, STR."HAS_PUBLISHED {in:\{timestamp}}", user, review);
        if(!ret) return false;

        node_types[0] = "Game";
        ret = neo4j.addRelationship(node_types, "HAS_REVIEW {numReports:0}", game, review);
        if(!ret) {
            node_types[0] = "User";
            neo4j.removeRelationship(node_types, "HAS_PUBLISHED", user, review);
            return false;
        }

        return true;
    }

    /**
     * Creates a new user
     * @param args game_name, username, rating, creation_date, content, published, votes
     * @return -1 if the review couldn't be added, 0 if already present, 1 if added
     */
    public int addReview(List<Object> args) {

        if(args.size() != 7) return -1;

        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> reviews = mongo.getCollection("reviews");

        String game = (String) args.getFirst();
        String uname = (String) args.get(1);
        String review_id = STR."\{game}|||\{uname}";

        //check if review is already present
        Document existingReview = reviews.find(eq("review", review_id))
                .projection(fields(include("_id"), excludeId()))
                .first();
        if(existingReview != null) return 0;

        Neo4jManager neo4j = Neo4jManager.getInstance();

        Document review = new Document();
        review.append("review", review_id);
        review.append("rating", args.get(2));
        review.append("creation_date", args.get(3));
        review.append("content", args.get(4));
        review.append("published", args.get(5));
        review.append("votes", args.get(6));

        mongo.addDoc("reviews", review);
        boolean ret = neo4j.addNode("Review", review_id);
        if(!ret) {
            mongo.removeDoc("reviews", "review", review_id);
            return -1;
        }

        ret = addGameToOwned(uname, game);
        if(!ret) return -1;
        ret = addReviewToPublished(uname, game, review_id);
        if(!ret) return -1;

        return 1;
    }

    public boolean removeReview(String review) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.removeDoc("reviews", "review", review);
        neo4j.removeNode("Review", review);

        return true;
    }

    public ArrayList<ArrayList<Object>> getFriendList(String user, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        return neo4j.getGenericList(node_types, "IS_FRIEND_WITH", user, offset);
    }

    public ArrayList<ArrayList<Object>> getGameList(String user, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "Game"};
        return neo4j.getGenericList(node_types, "OWNS", user, offset);
    }

    public ArrayList<ArrayList<Object>> getReviewList(String game, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"Game", "Review"};
        return neo4j.getGenericList(node_types, "HAS_REVIEW", game, offset);
    }

}
