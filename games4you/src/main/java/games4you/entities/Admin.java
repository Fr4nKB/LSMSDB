package games4you.entities;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
        boolean ret = neo4j.addNode("Game", gid);
        if(!ret) {
            mongo.removeDoc(false,"games", "name", game_name);
            return false;
        }
        else {
            neo4j.addAttribute("Game", gid, "tags", args.get(2));
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

    public ArrayList<Object> getReportedReviews(int offset) {
        MongoManager mongo = MongoManager.getInstance();
        try {
            MongoCollection<Document> coll = mongo.getCollection("reviews");
            MongoCursor<Document> cur = coll
                    .find(Filters.ne("reporters", Collections.emptyList()))
                    .projection(Projections.fields(Projections.include("review", "reporters"), Projections.excludeId()))
                    .sort(Sorts.descending("creation_date"))
                    .skip(offset)
                    .limit(20)
                    .iterator();

            ArrayList<Object> list = new ArrayList<>();
            while(cur.hasNext()) {
                list.add(cur.next());
            }
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean evaluateReportedReview() {
        return true;
    }

}
