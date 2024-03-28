package games4you;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import games4you.manager.MongoManager;
import games4you.manager.Neo4jManager;
import org.bson.Document;

import javax.print.Doc;
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
            game.append("description", (String) args.get(3));
            game.append("header_image", (String) args.get(4));
        }

        mongo.addElem("games", game);
        boolean ret = neo4j.addGame(game_name);
        if(!ret) {
            mongo.removeElem("games", "name", game_name);
            return false;
        }
        return true;
    }
}
