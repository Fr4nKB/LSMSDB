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

import java.util.*;


public class Admin extends User {
    public boolean addGame(HashMap<String, Object> args) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        Document game = new Document();
        int gid = -1;
        String game_name = "";
        try {
            gid = (Integer) args.get("gid");
            game_name = (String) args.get("name");
            game.append("gid", gid);
            game.append("name", game_name);
            game.append("tags", args.get("tags"));
            game.append("release_date", args.get("release_date"));
            if(args.size() > 3) {
                game.append("latestReviews", args.get("latestReviews"));
                if(args.size() == 7) {
                    game.append("description", args.get("description"));
                    game.append("header_image", args.get("header_image"));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        //check if game already present
        if(mongo.findDocumentByKeyValue("games", "name", game_name).hasNext()) return false;

        mongo.addDoc("games", game);

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", gid);
        map.put("name", game_name);
        boolean ret = neo4j.addNode("Game", map);

        if(!ret) {
            mongo.removeDoc(false,"games", "name", game_name);
            return false;
        }

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

    public boolean removeGame(int gid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        boolean ret = mongo.removeDoc(false, "games", "gid", gid);
        if(!ret) return false;
        ret = mongo.removeDoc(true, "reviews", "gid", gid);
        if(!ret) return false;
        ret = neo4j.removeSubNodes("Game", "HAS_REVIEW", "Review", gid);
        if(!ret) return false;
        return neo4j.removeNode("Game", gid);
    }

    public boolean banGamer(int uid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        // retrieve user to ban (blacklist) him/her
        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("users", "uid", uid);
        if(!cur.hasNext()) return false;

        Document user = cur.next();
        Document banned_user = new Document();

        banned_user.append("firstname", user.getString("firstname"));
        banned_user.append("lastname", user.getString("lastname"));
        banned_user.append("datebirth", user.getString("datebirth"));
        banned_user.append("uname", user.getString("uname"));
        mongo.addDoc("blacklist", banned_user);

        // remove the user and all his/her reviews
        boolean ret = mongo.removeDoc(false, "users", "uid", uid);
        if(!ret) return false;
        ret = mongo.removeDoc(true, "reviews", "uid", uid);
        if(!ret) return false;
        ret = neo4j.removeSubNodes("User", "HAS_PUBLISHED", "Review", uid);
        if(!ret) return false;
        return neo4j.removeNode("User", uid);
    }

    public ArrayList<String> getReportedReviews(int offset) {
        MongoManager mongo = MongoManager.getInstance();
        try {
            MongoCollection<Document> coll = mongo.getCollection("reviews");
            MongoCursor<Document> cur = coll
                    .find(Filters.exists("reports"))
                    .projection(Projections.fields(Projections.excludeId()))
                    .sort(Sorts.descending("creation_date"))
                    .skip(offset)
                    .limit(20)
                    .iterator();

            ArrayList<String> list = new ArrayList<>();
            while(cur.hasNext()) {
                Document elem = cur.next();
                list.add(elem.toJson());
            }
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean evaluateReportedReview(int rid, boolean judgment) {
        if(!judgment) {
            return super.removeReview(rid);
        }
        else {
            MongoManager mongo = MongoManager.getInstance();
            MongoCollection<Document> coll = mongo.getCollection("reviews");

            UpdateResult res = coll.updateOne(
                    Filters.eq("rid", rid),
                    Updates.unset("reports")
            );
            return res.getModifiedCount() > 0;
        }
    }

}
