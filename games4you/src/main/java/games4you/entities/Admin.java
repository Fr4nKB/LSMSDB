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
import org.bson.Document;

import java.util.*;


public class Admin extends User {
    public boolean insertGame(HashMap<String, Object> args) {
        Game game = new Game();
        return game.insertGame(args);
    }

    public boolean deleteGame(int gid) {
        Game game = new Game();
        return game.deleteGame(gid);
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
