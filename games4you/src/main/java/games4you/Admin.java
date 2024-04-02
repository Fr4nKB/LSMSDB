package games4you;

import com.mongodb.client.MongoCursor;
import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.util.List;

public class Admin {
    public boolean addGame(List<Object> args) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        String game_name = (String) args.getFirst();

        //check if game already present
        if(mongo.findDocumentByKeyValue("games", "name", game_name).hasNext()) return false;

        Document game = new Document();
        game.append("name", game_name);
        game.append("tags", args.get(1));
        game.append("release_date", args.get(2));
        if(args.size() > 3) {
            game.append("description", args.get(3));
            game.append("header_image", args.get(4));
        }

        mongo.addDoc("games", game);
        boolean ret = neo4j.addNode("Game", game_name);
        if(!ret) {
            mongo.removeDoc(false,"games", "name", game_name);
            return false;
        }
        else {
            neo4j.addAttribute("Game", game_name, "tags", args.get(1));
        }
        return true;
    }

    public void removeGame(String game) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        mongo.removeDoc(false, "games", "name", game);
        mongo.removeDoc(true, "reviews", "game", game);
        neo4j.removeSubNodes("Game", "HAS_REVIEW", "Review", game);
        neo4j.removeNode("Game", game);
    }

    public void banUser(String uname) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        // retrieve user to ban (blacklist) him/her
        MongoCursor<Document> cur = mongo.findDocumentByKeyValue("users", "uname", uname);
        if(!cur.hasNext()) return;

        Document user = cur.next();
        Document banned_user = new Document();

        banned_user.append("firstname", user.getString("firstname"));
        banned_user.append("lastname", user.getString("lastname"));
        banned_user.append("datebirth", user.getString("datebirth"));
        banned_user.append("uname", user.getString("uname"));
        mongo.addDoc("blacklist", banned_user);

        // remove the user and all his/her reviews
        mongo.removeDoc(false, "users", "uname", uname);
        mongo.removeDoc(true, "reviews", "uname", uname);
        neo4j.removeNode("User", uname);
    }
}
