package webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.util.Populator;

import java.io.IOException;

public class Populate {
    public static void main(String[] args) {

        //clean databases
        try {
            ProcessBuilder pbMongo = new ProcessBuilder("mongosh", "--eval", "use games4you", "--eval", "db.dropDatabase()");
            Process processMongo = pbMongo.start();
            processMongo.waitFor();

            ProcessBuilder pbNeo4j = new ProcessBuilder("cypher-shell", "-u", "neo4j", "-p", "password", "MATCH(n) OPTIONAL MATCH (n)-[r]-() DELETE n,r;");
            Process processNeo4j = pbNeo4j.start();
            processNeo4j.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        Populator pop = new Populator();
        Gamer gamer = new Gamer();
        Admin admin = new Admin();

        assert pop.populateGamers() == 5;
        pop.populateGames();
        pop.populateReviews();

        //FRIENDS
        assert gamer.sendRequest(0, 1): "Friend 1 not sent";
        assert gamer.acceptRequest(1, 0): "Friend 1 not added";
        assert gamer.sendRequest(0, 2): "Friend 2 not sent";
        assert gamer.acceptRequest(2, 0): "Friend 2 not added";
        assert gamer.sendRequest(1, 2): "Friend 3 not sent";
        assert gamer.acceptRequest(2, 1): "Friend 3 not added";
        assert gamer.sendRequest(2, 3): "Friend 4 not sent";
        assert gamer.acceptRequest(3, 2): "Friend 4 not added";
        assert gamer.sendRequest(3, 0): "Friend 5 not sent";
        assert gamer.acceptRequest(0, 3): "Friend 5 not added";

        assert gamer.addGameToLibrary(0,0) == 1;
        assert gamer.addGameToLibrary(0,1) == 1;
        assert gamer.addGameToLibrary(1,1) == 1;
        assert gamer.addGameToLibrary(1,2) == 1;
        assert gamer.updatePlayedHours(1, 2, 10);
        assert gamer.addGameToLibrary(2,4) == 1;
        assert gamer.addGameToLibrary(3,4) == 1;

        assert !gamer.reportReview(1, 3);
        assert gamer.reportReview(1, 4);
        assert gamer.reportReview(4, 3);
        assert !gamer.reportReview(4, 3);

    }

}
