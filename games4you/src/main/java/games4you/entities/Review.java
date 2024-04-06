package games4you.entities;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.BsonDocument;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

public class Review {

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
        long timestamp = Instant.now().getEpochSecond();
        String query = String.format(
                "MATCH (u:User {id: %d}), (g:Game {id: %d}), (r:Review {id: %d}) " +
                        "MERGE (u)-[:HAS_WROTE {in:%d}]->(r) " +
                        "MERGE (g)-[:HAS_REVIEW {votes:0}]->(r) " +
                        "MERGE (u)-[:OWNS {hours:0}]->(g)",
                uid, gid, rid, timestamp);
        neo4j.executeWriteTransactionQuery(query);

        //used to initially populate the db
        if(votes > 0) {
            String[] node_type = {"Game", "Review"};
            int[] node_name = {gid, rid};
            neo4j.incAttribute(node_type, node_name, "HAS_REVIEW", "votes", votes);
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

    /**
     * Retrieves the most recent reviews for a game or user and adds them to relative document
     * @param collection either games or users
     * @param id the gid or uid
     * @param amount the number of reviews to collect
     * @return true if the reviews are added, false otherwise
     */
    private boolean updateRedundantReviews(String collection, int id, int amount) {
        String id_name = "";
        if(collection.equals("games")) id_name = "gid";
        else if(collection.equals("users")) id_name = "uid";
        else return false;
        if(amount <= 0) return false;

        MongoManager mongo = MongoManager.getInstance();

        //find 3 most recent reviews
        MongoCollection<Document> coll = mongo.getCollection("reviews");
        MongoCursor<Document> cur = coll.find(
                        Filters.eq(id_name, id))
                .projection(Projections.exclude("_id", "reports"))
                .sort(Sorts.descending("creation_date"))
                .limit(amount)
                .iterator();

        List<BsonDocument> list = new ArrayList<>();
        while(cur.hasNext()) {
            list.add(mongo.convert2BsonDoc(cur.next()));
        }
        if(list.isEmpty()) return false;

        coll = mongo.getCollection(collection);
        UpdateResult res = coll.updateOne(
                Filters.eq(id_name, id),
                Updates.set("latestReviews", list)
        );

        return res.getModifiedCount() > 0;
    }
}
