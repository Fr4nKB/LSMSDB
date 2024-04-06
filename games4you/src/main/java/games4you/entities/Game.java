package games4you.entities;

import games4you.dbmanager.MongoManager;
import games4you.dbmanager.Neo4jManager;
import org.bson.Document;

import java.time.Instant;
import java.util.HashMap;

public class Game {
    public boolean insertGame(HashMap<String, Object> args) {
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
            if (args.size() > 3) {
                game.append("latestReviews", args.get("latestReviews"));
                if (args.size() == 7) {
                    game.append("description", args.get("description"));
                    game.append("header_image", args.get("header_image"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        //check if game already present
        if (mongo.findDocumentByKeyValue("games", "name", game_name).hasNext()) return false;

        mongo.addDoc("games", game);

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", gid);
        map.put("name", game_name);
        boolean ret = neo4j.addNode("Game", map);

        if (!ret) {
            mongo.removeDoc(false, "games", "name", game_name);
            return false;
        }
        return true;
    }


    public boolean deleteGame(int gid) {
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


    public boolean addGameToLibrary(int gid, int uid) {
        //create relationships between review, game and gamer
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                "MATCH (u:User {id: %d}), (g:Game {id: %d})" +
                        "MERGE (u)-[:OWNS {hours:0}]->(g)",
                uid, gid);
        neo4j.executeWriteTransactionQuery(query);
        return true;
    }

    public boolean removeGameFromLibrary(int gid, int uid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String[] node_types = new String[]{"User", "Game"};
        return neo4j.removeRelationship(node_types, "OWNS", uid, gid);
    }
}
