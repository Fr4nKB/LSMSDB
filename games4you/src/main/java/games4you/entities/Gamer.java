package games4you.entities;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

public class Gamer extends User {

    private ArrayList<Object> getRelationshipList(String query) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public boolean addFriend(int uid1, int uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String[] node_types = new String[]{"User", "User"};
        long timestamp = Instant.now().getEpochSecond();
        String relation = String.format("IS_FRIEND_WITH {since: %d}", timestamp);

        return neo4j.addRelationship(node_types, relation, uid1, uid2);
    }

    public boolean removeFriend(int uid1, int uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        return neo4j.removeRelationship(node_types, "IS_FRIEND_WITH", uid1, uid2);
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

    public boolean upvoteReview(int rid) {
        MongoManager mongo = MongoManager.getInstance();
        return mongo.incVote(rid);
    }

    public boolean reportReview(int uid, int rid) {
        MongoManager mongo = MongoManager.getInstance();

        //retrieve review
        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("reviews", "rid", rid);
        if(!cur.hasNext()) return false;

        Document review = cur.next();
        //check if review creator is not the reporter itself
        int reported_uid = (Integer) review.getInteger("uid");
        if (uid == reported_uid) return false;

        //if the review has already been reported before, check if the reporter has already reported
        if(review.containsKey("reports")) {
            HashMap<String, Object> reports = new HashMap<String, Object>((Document) review.get("reports"));
            ArrayList<Integer> reporters = (ArrayList<Integer>) reports.get("reporters");
            if(reporters.contains(uid)) return false;
        }

        return mongo.addReporter(rid, uid);
    }

    public boolean updatePlayedHours(int uid, int gid, int amount) {
        if(amount <= 0) return false;

        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String [] node_types = {"User", "Game"};
        int [] node_names = {uid, gid};

        MongoCollection<Document> coll = mongo.getCollection("games");
        String game_name = (String) coll.find(
                Filters.eq("gid", gid))
                .projection(Projections.include("name"))
                .first().get("name");

        if(!neo4j.incAttribute(node_types, node_names, "OWNS", "hours", amount)) return false;

        Document doc = new Document();
        doc.put("gid", gid);
        doc.put("name", game_name);
        coll = mongo.getCollection("users");
        UpdateResult res = coll.updateOne(
                Filters.eq("uid", uid),
                Updates.set("lastGamePlayed", mongo.convert2BsonDoc(doc))
        );

        return res.getModifiedCount() > 0;
    }

    public ArrayList<Object> getFriendList(int uid, int offset) {
        String query = String.format(
                "MATCH (:User {id: %d})-[r:IS_FRIEND_WITH]->(b:User) " +
                        "RETURN {uid: b.id, uname: b.uname, since: r.since} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getGameList(int uid, int offset) {
        String query = String.format(
                "MATCH (:User {id: %d})-[r:OWNS]->(b:Game) " +
                        "RETURN {gid: b.id, game: b.name, hours: r.hours} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getReviewList(int gid, int offset) {
        String query = String.format(
                "MATCH (:Game {id: %d})-[r:HAS_REVIEW]->(b:Review) " +
                        "RETURN {rid: b.id, game: b.game, uname: b.uname, votes: r.votes} AS result " +
                        "SKIP %d LIMIT %d",
                gid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> homePage(int uid, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                "MATCH (u:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)\n" +
                        "MATCH (f)-[r:IS_FRIEND_WITH]-(fof:User)\n" +
                        "WHERE fof <> u\n" +
                        "WITH f, r, fof\n" +
                        "ORDER BY r.since DESC\n" +
                        "RETURN {type: \"F\", friend: f.uname, time: r.since, object: fof.uname} AS result\n" +
                        "UNION\n" +
                        "MATCH (:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)-[r:HAS_WROTE]->(rev:Review)\n" +
                        "WITH f, r, rev\n" +
                        "ORDER BY r.in DESC\n" +
                        "RETURN {type: \"R\", friend: f.uname, time: r.in, object: rev.game} AS result\n" +
                        "SKIP %d LIMIT 20",
                uid, uid, offset);

        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> tagsRecommendationNORED(int uid) {
        String query = String.format(
                "MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(fof), (fof)-[:OWNS]->(g:Game)\n" +
                        "UNWIND g.tags AS tag\n" +
                        "WITH u, tag, count(*) AS tagCount\n" +
                        "ORDER BY tagCount DESC LIMIT 5\n" +
                        "WITH u, collect(tag) AS topTags\n" +
                        "MATCH (game:Game)\n" +
                        "WHERE NOT((u)-[:OWNS]->(game)) AND ANY(t IN game.tags WHERE t IN topTags)\n" +
                        "RETURN game.name AS RecommendedGame, [t IN game.tags WHERE t IN topTags] AS Tags\n" +
                        "ORDER BY size(Tags) DESC LIMIT 5",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> tagsRecommendationRED(int uid) {
        String query = String.format(
                "MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(friend:User)\n" +
                        "WITH u, friend\n" +
                        "UNWIND friend.tags AS tag\n" +
                        "WITH u, tag, COUNT(DISTINCT friend) AS tagCount\n" +
                        "ORDER BY tagCount DESC\n" +
                        "LIMIT 5\n" +
                        "WITH u, COLLECT(tag) AS topFriendTags\n" +
                        "MATCH (game:Game)\n" +
                        "WHERE NOT((u)-[:OWNS]->(game)) AND ANY(tag IN game.tags WHERE tag IN topFriendTags)\n" +
                        "RETURN game.name",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }


}
