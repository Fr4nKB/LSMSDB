package games4you;

import games4you.manager.MongoManager;
import games4you.manager.Neo4jManager;
import org.bson.Document;

public class Admin {
    public boolean addGame(String[] args) {
        MongoManager mongo = MongoManager.getInstance();
        Neo4jManager neo4j = Neo4jManager.getInstance();

        //check if game already present
        if(mongo.findDocumentByKeyValue("games", "name", args[0]).hasNext()) return false;

        Document game = new Document();
        game.append("name", args[0]);
        game.append("tags", args[1]);
        game.append("release_date", args[2]);
        if(args.length > 3) {
            game.append("description", args[3]);
            game.append("header_image", args[4]);
        }

        mongo.addElem("games", game);
        boolean ret = neo4j.addGame(args[0]);
        if(!ret) {
            mongo.removeElem("games", "name", args[0]);
            return false;
        }
        return true;
    }
}
