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
        Review review = new Review();
        return review.addReview(args);
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
        long reported_uid = review.getLong("uid");
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
        long[] node_names = {uid, gid};

        MongoCollection<Document> coll = mongo.getCollection("games");
        Document game = coll.find(
                Filters.eq("gid", gid))
                .projection(Projections.include("name"))
                .first();
        if(game == null) return false;
        String game_name = game.getString("name");

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

    public ArrayList<Object> getFriendList(long uid, int offset) {
        String query = String.format(
                "MATCH (:User {id: %d})-[r:IS_FRIEND_WITH]->(b:User) " +
                        "RETURN {uid: b.id, uname: b.uname, since: r.since} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getGameList(long uid, int offset) {
        String query = String.format(
                "MATCH (:User {id: %d})-[r:OWNS]->(b:Game) " +
                        "RETURN {gid: b.id, game: b.name, hours: r.hours} AS result " +
                        "SKIP %d LIMIT %d",
                uid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> getReviewList(long gid, int offset) {
        String query = String.format(
                "MATCH (:Game {id: %d})-[r:HAS_REVIEW]->(b:Review) " +
                        "RETURN {rid: b.id, game: b.game, uname: b.uname, votes: r.votes} AS result " +
                        "SKIP %d LIMIT %d",
                gid, offset, 20);
        return getRelationshipList(query);
    }

    public ArrayList<Object> homePage(long uid, int offset) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                """
                CALL {
                    MATCH (:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)-[r:HAS_WROTE]->(rev:Review)
                    WITH f, r, rev
                    ORDER BY r.in DESC
                    RETURN {type: "R", friend: {id: f.id, name: f.uname}, time: r.in, object: {rid: rev.id, gid: rev.gid, name: rev.game}} AS result
                    UNION
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)
                    MATCH (f)-[r:IS_FRIEND_WITH]-(fof:User)
                    WHERE fof <> u
                    WITH f, r, fof
                    ORDER BY r.since DESC
                    RETURN {type: "F", friend: {id: f.id, name: f.uname}, time: r.since, object: {id: f.id, name: fof.uname}} AS result
                    }
                    RETURN result SKIP %d LIMIT 20""",
                uid, uid, offset);

        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> tagsRecommendationNORED(long uid) {
        String query = String.format(
                """
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(fof), (fof)-[:OWNS]->(g:Game)
                    UNWIND g.tags AS tag
                    WITH u, tag, count(*) AS tagCount
                    ORDER BY tagCount DESC LIMIT 5
                    WITH u, collect(tag) AS topTags
                    MATCH (game:Game)
                    WHERE NOT((u)-[:OWNS]->(game)) AND ANY(t IN game.tags WHERE t IN topTags)
                    RETURN game.name AS RecommendedGame, [t IN game.tags WHERE t IN topTags] AS Tags
                    ORDER BY size(Tags) DESC LIMIT 5""",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> tagsRecommendationRED(long uid) {
        String query = String.format(
                """
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(friend:User)
                    WITH u, friend
                    UNWIND friend.tags AS tag
                    WITH u, tag, COUNT(DISTINCT friend) AS tagCount
                    ORDER BY tagCount DESC
                    LIMIT 5
                    WITH u, COLLECT(tag) AS topFriendTags
                    MATCH (game:Game)
                    WHERE NOT((u)-[:OWNS]->(game)) AND ANY(tag IN game.tags WHERE tag IN topFriendTags)
                    RETURN game.name""",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    public boolean addGameToLibrary(long uid, long gid){
        Game game = new Game();
        return game.addGameToLibrary(uid, gid);
    }

    public boolean removeGameFromLibrary(long uid, long gid){
        Game game = new Game();
        return game.removeGameFromLibrary(uid, gid);
    }

}
