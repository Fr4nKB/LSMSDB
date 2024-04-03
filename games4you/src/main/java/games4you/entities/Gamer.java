package games4you.entities;

import com.mongodb.client.MongoCursor;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;

public class Gamer extends User {

    private ArrayList<String> getRelationshipList(String query) {
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

        //check if review creator is not the reporter itself and if it's the first time the user reports this review
        Document review = cur.next();
        int reported_uid = (Integer) review.getInteger("uid");
        ArrayList<Integer> reporters = (ArrayList<Integer>) review.get("reporters");
        if(reporters.contains(uid) || uid == reported_uid) return false;

        return mongo.addReporter(rid, uid);
    }

    public boolean updatePlayedHours(int uid, int gid, int amount) {
        if(amount <= 0) return false;
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String [] node_types = {"User", "Game"};
        int [] node_names = {uid, gid};
        return neo4j.incAttribute(node_types, node_names, "OWNS", "hours", amount);
    }

    public ArrayList<String> getFriendList(int uid, int offset) {
        String query = String.format(
                "MATCH (a:User {id: %d})-[:IS_FRIEND_WITH]->(b:User) RETURN b.name SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<String> getGameList(int uid, int offset) {
        String query = String.format(
                "MATCH (a:User {id: %d})-[:OWNS]->(b:Game) RETURN b.name SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<String> getReviewList(int gid, int offset) {
        String query = String.format(
                "MATCH (a:Game {id: %d})-[:HAS_REVIEW]->(b:Review) RETURN b.name SKIP %d LIMIT %d",
                gid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<String> tagsRecommendationNORED(int uid) {
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

    public ArrayList<String> tagsRecommendationRED(int uid) {
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
