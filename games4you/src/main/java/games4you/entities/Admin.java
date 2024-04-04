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
import org.neo4j.driver.Result;

import java.util.*;


public class Admin extends User {
    public boolean addGame(List<Object> args) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        int gid = (Integer) args.getFirst();
        String game_name = (String) args.get(1);

        //check if game already present
        if(mongo.findDocumentByKeyValue("games", "name", game_name).hasNext()) return false;

        Document game = new Document();
        game.append("name", game_name);
        game.append("tags", args.get(2));
        game.append("release_date", args.get(3));
        if(args.size() > 4) {
            game.append("description", args.get(4));
            game.append("header_image", args.get(5));
        }
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

    public void removeGame(int gid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.removeDoc(false, "games", "gid", gid);
        mongo.removeDoc(true, "reviews", "gid", gid);
        neo4j.removeSubNodes("Game", "HAS_REVIEW", "Review", gid);
        neo4j.removeNode("Game", gid);
    }

    public void banGamer(int uid) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        // retrieve user to ban (blacklist) him/her
        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("users", "uid", uid);
        if(!cur.hasNext()) return;

        Document user = cur.next();
        Document banned_user = new Document();

        banned_user.append("firstname", user.getString("firstname"));
        banned_user.append("lastname", user.getString("lastname"));
        banned_user.append("datebirth", user.getString("datebirth"));
        banned_user.append("uname", user.getString("uname"));
        mongo.addDoc("blacklist", banned_user);

        // remove the user and all his/her reviews
        mongo.removeDoc(false, "users", "uid", uid);
        mongo.removeDoc(true, "reviews", "uid", uid);
        neo4j.removeSubNodes("User", "HAS_PUBLISHED", "Review", uid);
        neo4j.removeNode("User", uid);
    }

    public int publishReview(long rid, boolean judgment) {
        MongoManager mongo = MongoManager.getInstance();
        if(!judgment) {
            mongo.removeDoc(false, "uncheckedReviews", "rid", rid);
            return 1;
        }
        else {
            MongoCursor<Document> cur = mongo.findDocumentByKeyValue("uncheckedReviews", "rid", rid);
            if(!cur.hasNext()) return -1;
            mongo.removeDoc(false, "uncheckedReviews", "rid", rid);

            //convert the document into an arraylist
            Document doc = cur.next();
            ArrayList<Object> ret = new ArrayList<>(doc.values());
            ret.removeFirst();  //remove old _id
            ret.add(8, true); ret.add(9, 0);  //add published=true and votes=0

            return super.addReview(ret);
        }

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
