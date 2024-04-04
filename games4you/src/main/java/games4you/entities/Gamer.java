package games4you.entities;

import com.mongodb.client.MongoCursor;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

public class Gamer extends User {

    private ArrayList<Object> getRelationshipList(String query) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public boolean addFriend(int uid1, int uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        return neo4j.addRelationship(node_types, STR."IS_FRIEND_WITH {since:\{timestamp}}", uid1, uid2);
    }

    public boolean removeFriend(int uid1, int uid2) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "User"};
        return neo4j.removeRelationship(node_types, "IS_FRIEND_WITH", uid1, uid2);
    }

    public boolean writeReview(int uid, int rid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        long timestamp = Instant.now().getEpochSecond();
        String query = String.format(
                "MATCH (u:User {id: %d}), (r:Review {id: %d}) " +
                        "MERGE (u)-[:HAS_WROTE {in: %d}]->(r)",
                uid, rid, timestamp);
        return neo4j.executeWriteTransactionQuery(query);
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
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String [] node_types = {"User", "Game"};
        int [] node_names = {uid, gid};
        return neo4j.incAttribute(node_types, node_names, "OWNS", "hours", amount);
    }

    public ArrayList<Object> getFriendList(int uid, int offset) {
        String query = String.format(
                "MATCH (a:User {id: %d})-[:IS_FRIEND_WITH]->(b:User) " +
                        "RETURN {uid: b.id, uname: b.uname} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getGameList(int uid, int offset) {
        String query = String.format(
                "MATCH (a:User {id: %d})-[:OWNS]->(b:Game) " +
                        "RETURN {gid: b.id, game: b.name} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getReviewList(int gid, int offset) {
        String query = String.format(
                "MATCH (a:Game {id: %d})-[:HAS_REVIEW]->(b:Review) " +
                        "RETURN {rid: b.id, game: b.game, uname: b.uname} AS result " +
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
