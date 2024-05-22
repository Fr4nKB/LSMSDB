package games4you.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import games4you.util.Constants;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Gamer extends User {


    public String checkFriendshipStatus(long uid1, long uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();

        // check if user are already friends, in that case return since when
        String query = String.format(
                """
                        MATCH (u:User {id: %d})-[r:IS_FRIEND_WITH]-(f:User {id: %d})
                        RETURN {since: r.since} LIMIT 1
                        """,
                uid1, uid2
        );
        ArrayList<Object> res = neo4j.getQueryResultAsList(query);
        if(!res.isEmpty()) return (String) res.get(0);


        // check if friendship is pending, in that case return who asked it
        query = String.format(
                """
                        MATCH p=(u1:User {id: %d})-[r:PENDING]-(u2:User {id: %d})
                        WITH CASE WHEN p IS NOT NULL THEN startNode(r).id ELSE NULL END AS originNode
                        RETURN {origin: originNode} LIMIT 1
                        """,
                uid1, uid2
        );
        res = neo4j.getQueryResultAsList(query);
        if(!res.isEmpty()) return (String) res.get(0);

        return "{}";
    }

    public String checkGameRelationship(long uid, long gid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();

        // check if user already owns the game or if he/she has reviewed it
        String query = String.format(
                """
                        MATCH(u:User {id: %d}), (g:Game {id: %d})
                            OPTIONAL MATCH (u)-[:HAS_WROTE]->(r:Review {gid: %d})
                            OPTIONAL MATCH (u)-[o:OWNS]->(g)
                            RETURN {rev: {id: r.id, in: r.creation}, hours: o.hours} LIMIT 1
                        """,
                uid, gid, gid
        );
        ArrayList<Object> res = neo4j.getQueryResultAsList(query);
        if(!res.isEmpty()) return (String) res.get(0);
        else return "{}";
    }

    public String checkReviewRelationship(long uid, long rid) {
        MongoManager mongo = MongoManager.getInstance();

        MongoCursor<Document> cur = mongo.findDocumentsByKeyValue("reviews", "rid", rid);
        if(!cur.hasNext()) return "{}";
        Document review = cur.next();

        HashMap<String, Boolean> ret = new HashMap<String, Boolean>();

        ArrayList<Long> likes = (ArrayList<Long>) review.get("upvotes");
        if(!likes.contains(uid)) ret.put("upvote", false);
        else ret.put("upvote", true);

        if(review.containsKey("reports")) {
            HashMap<String, Object> reports = new HashMap<String, Object>((Document) review.get("reports"));
            ArrayList<Long> reporters = (ArrayList<Long>) reports.get("reporters");
            if(!reporters.contains(uid)) ret.put("report", false);
            else ret.put("report", true);
        }
        else ret.put("report", false);

        ObjectMapper objMapper = new ObjectMapper();
        try {
            return objMapper.writeValueAsString(ret);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public boolean sendRequest(long uid1, long uid2) {
        String res = checkFriendshipStatus(uid1, uid2);
        if(res.equals("{}")) {
            Neo4jManager neo4j = Neo4jManager.getInstance();
            String[] node_types = new String[]{"User", "User"};
            return neo4j.addRelationship(node_types, "PENDING", uid1, uid2) == 1;
        }
        else return false;
    }

    public boolean revokeRequest(long uid1, long uid2) {
        String res = checkFriendshipStatus(uid1, uid2);
        if(res != null && res.startsWith("{\"origin\":")) {
            Neo4jManager neo4j = Neo4jManager.getInstance();
            String[] node_types = new String[]{"User", "User"};
            return neo4j.removeRelationship(node_types, "PENDING", uid1, uid2) == 1;
        }
        return false;
    }

    public boolean acceptRequest(long uid1, long uid2) {
        String res = checkFriendshipStatus(uid1, uid2);
        if(res != null && res.startsWith("{\"origin\":")) {
            Neo4jManager neo4j = Neo4jManager.getInstance();

            revokeRequest(uid2, uid1);

            String[] node_types = new String[]{"User", "User"};
            long timestamp = Instant.now().getEpochSecond();
            String relation = String.format("IS_FRIEND_WITH {since: %d}", timestamp);
            return neo4j.addRelationship(node_types, relation, uid2, uid1) == 1;
        }
        else return false;
    }

    public boolean declineRequest(long uid1, long uid2) {
        return revokeRequest(uid2, uid1);
    }

    public ArrayList<Object> getRequestsList(long uid, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        String query = String.format("""
                        MATCH(u:User)-[:PENDING]->(n:User {id: %d})
                        RETURN {uid: u.id, uname: u.uname}
                        SKIP %d LIMIT %d
                        """,
                uid, offset, limit
        );

        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public boolean removeFriend(long uid1, long uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        return neo4j.removeRelationship(node_types, "IS_FRIEND_WITH", uid1, uid2) == 1;
    }

    /**
     * Creates a new user
     * @param args game_name, username, rating, creation_date, content, published, votes
     * @return -1 if the review couldn't be added, 0 if already present, 1 if added
     */
    public int addReview(HashMap<String, Object> args) {
        Review review = new Review();
        return review.addReview(args);
    }

    public boolean reportReview(long uid, long rid) {
        String res = checkReviewRelationship(uid, rid);

        if(res == null) return false;
        else if(!res.equals("{}")) {
            ObjectMapper objectMapper = new ObjectMapper();
            HashMap<String, Boolean> map;
            try {
                map = objectMapper.readValue(res, HashMap.class);
                if(map.get("report")) return false;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        MongoManager mongo = MongoManager.getInstance();

        //retrieve review
        MongoCursor<Document> cur = mongo.findDocumentsByKeyValue("reviews", "rid", rid);
        if(!cur.hasNext()) return false;

        Document review = cur.next();
        //check if review creator is not the reporter itself
        long reported_uid = review.getLong("uid");
        if (uid == reported_uid) return false;

        long timestamp = Instant.now().getEpochSecond();
        MongoCollection<Document> coll = mongo.getCollection("reviews");
        UpdateResult upRes = coll.updateOne(
                Filters.eq("rid", rid),
                Updates.combine(
                        Updates.set("reports.lastRep", timestamp),
                        Updates.inc("reports.numRep", 1),
                        Updates.push("reports.reporters", uid)));

        return upRes.getModifiedCount() > 0;
    }

    public boolean upvoteReview(long uid, long rid) {
        String res = checkReviewRelationship(uid, rid);

        if(res == null) return false;
        else if(!res.equals("{}")) {
            ObjectMapper objectMapper = new ObjectMapper();
            HashMap<String, Boolean> map;
            try {
                map = objectMapper.readValue(res, HashMap.class);
                if(map.get("upvote")) return false;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        MongoManager mongo = MongoManager.getInstance();
        //retrieve review
        MongoCursor<Document> cur = mongo.findDocumentsByKeyValue("reviews", "rid", rid);
        if(!cur.hasNext()) return false;

        Document review = cur.next();
        //check if review creator is not the reporter itself
        long reported_uid = review.getLong("uid");
        if (uid == reported_uid) return false;

        MongoCollection<Document> coll = mongo.getCollection("reviews");
        if(coll == null) return false;

        UpdateResult upRes = coll.updateOne(
                Filters.eq("rid", rid),
                Updates.combine(
                        Updates.inc("numUpvotes", 1),
                        Updates.push("upvotes", uid)
                ));

        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_type = {"Review"}; long[] node_name = {rid};
        neo4j.incAttribute(node_type, node_name, "", "numUpvotes", 1);

        return upRes.getModifiedCount() > 0;
    }

    public boolean updatePlayedHours(long uid, long gid, int amount) {
        if(amount <= 0) return false;

        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String [] node_types = {"User", "Game"};
        long[] node_names = {uid, gid};

        String game_name = retrieveGameName(gid);

        if(!neo4j.incAttribute(node_types, node_names, "OWNS", "hours", amount)) return false;

        Document doc = new Document();
        doc.put("gid", gid);
        doc.put("name", game_name);
        MongoCollection<Document> coll = mongo.getCollection("users");
        UpdateResult res = coll.updateOne(
                Filters.eq("uid", uid),
                Updates.set("lastGamePlayed", mongo.convert2BsonDoc(doc))
        );
        if(res.getMatchedCount() <= 0) return false;

        doc.put("uid", uid);
        doc.put("uname", retrieveUname(uid));
        doc.put("updatedAt", new Date());
        doc.put("hrs", amount);
        return mongo.addDoc("hottest", doc);
    }


    public ArrayList<Object> homePage(long uid, int offset, int limit) {
        if(limit <= 0) return null;
        if(limit > Constants.getMaxPagLim()) limit = Constants.getMaxPagLim();

        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                """
                CALL {
                    MATCH (:User {id: %d})-[:IS_FRIEND_WITH]-(f:User)-[:HAS_WROTE]->(rev:Review)
                    WITH f, rev
                    ORDER BY rev.creation DESC
                    RETURN {type: "R", friend: {id: f.id, name: f.uname}, time: rev.creation, object: {rid: rev.id, gid: rev.gid, name: rev.game}} AS result
                    UNION
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH]-(f:User)-[r:IS_FRIEND_WITH]-(fof:User)
                    WHERE fof <> u
                    WITH f, r, fof
                    ORDER BY r.since DESC
                    RETURN {type: "F", friend: {id: f.id, name: f.uname}, time: r.since, object: {id: fof.id, name: fof.uname}} AS result
                    }
                    WITH result ORDER BY result.time DESC
                    RETURN DISTINCT result SKIP %d LIMIT %d""",
                uid, uid, offset, limit);

        return neo4j.getQueryResultAsList(query);
    }

    public int addGameToLibrary(long uid, long gid) {
        Game game = new Game();
        return game.addGameToLibrary(uid, gid);
    }

    public int removeGameFromLibrary(long uid, long gid){
        Game game = new Game();
        return game.removeGameFromLibrary(uid, gid);
    }

    public String retrieveUname(long uid) {
        MongoManager mongo = MongoManager.getInstance();
        MongoCursor<Document> cur = mongo.findDocumentsByKeyValue("users", "uid", uid);
        if(!cur.hasNext()) return null;
        else {
            Document user = cur.next();
            return user.getString("uname");
        }
    }

    public String retrieveGameName(long gid) {
        MongoManager mongo = MongoManager.getInstance();
        MongoCursor<Document> cur = mongo.findDocumentsByKeyValue("games", "gid", gid);
        if(!cur.hasNext()) return null;
        else {
            Document game = cur.next();
            return game.getString("name");
        }
    }

}
