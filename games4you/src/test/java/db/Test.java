package db;

import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.util.Constants;
import games4you.util.NeoComplexQueries;
import games4you.util.Populator;

import java.io.IOException;

public class Test {
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
        NeoComplexQueries neoCQ = new NeoComplexQueries();

        assert pop.populateGamers() == 5;
        assert pop.populateGamers() == 0: "Users already present";
        pop.populateGames();
        pop.populateReviews();

        //LOGIN
        assert gamer.login("JohnnyTheDark", "trustNo1")[1] == 0: "Login failed";

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
        assert gamer.acceptRequest(3, 0): "Friend 5 not added";

        System.out.println(gamer.homePage(0, 0, Constants.getDefPagLim()));
        assert gamer.homePage(0, 0, Constants.getDefPagLim()).size() == 10: "Incorrect home page for reviews";

        System.out.println(gamer.getFriendList(0, 0, Constants.getDefPagLim()));
        assert gamer.getFriendList(0, 0, Constants.getDefPagLim()).size() == 3: "Incorrect friend list size";
        assert gamer.removeFriend(0, 1): "Friend 1 not removed";
        assert gamer.getFriendList(0, 0, Constants.getDefPagLim()).size() == 2: "Incorrect friend list size after removing one";

        assert gamer.sendRequest(0, 1): "Friend 1 couldn't be added again";

        //GAMES
        assert gamer.addGameToLibrary(0,0) == 1;
        assert gamer.addGameToLibrary(0,1) == 1;
        assert gamer.addGameToLibrary(1,1) == 1;
        assert gamer.addGameToLibrary(1,2) == 1;
        assert gamer.updatePlayedHours(1, 2, 10);
        assert gamer.addGameToLibrary(2,4) == 1;
        assert gamer.addGameToLibrary(3,4) == 1;
        System.out.println(gamer.getGameList(0,0, Constants.getDefPagLim()));
        assert gamer.getGameList(0, 0, Constants.getDefPagLim()).size() == 2: "Incorrect game list size";
        assert gamer.addGameToLibrary(0, 2) == 1;
        assert gamer.getGameList(0, 0, Constants.getDefPagLim()).size() == 3: "Incorrect game list size";
        assert gamer.removeGameFromLibrary(0, 2) == 1;
        assert gamer.getGameList(0, 0, Constants.getDefPagLim()).size() == 2: "Incorrect game list size";

        //REVIEWS
        System.out.println(gamer.getGameReviewList(1, 0));
        assert gamer.getGameReviewList(0, 0).size() == 2: "Incorrect review list size";
        assert gamer.removeReview(0);
        assert gamer.getGameReviewList(0, 0).size() == 1: "Incorrect review list size after removing one";

        assert !gamer.reportReview(1, 3);     //cannot report itself
        assert gamer.reportReview(1, 4);
        assert gamer.reportReview(4, 3);
        assert !gamer.reportReview(4, 3);    //cannot report twice

        //COMPLEX QUERIES
        System.out.println(neoCQ.friendsTagsBasedRecommendationNORED(0));
        assert neoCQ.friendsTagsBasedRecommendationNORED(0).size() == 2: "Incorrect recommendation list size";

        //ADMIN
        assert admin.banGamer(0): "User not banned";
        assert gamer.getFriendList(1, 0, Constants.getDefPagLim()).size() == 1;
        assert admin.deleteGame(1);
        assert !admin.deleteGame(1): "Game was already removed";
        assert gamer.getGameReviewList(1, 0).isEmpty();
        assert gamer.getGameList(1, 0, Constants.getDefPagLim()).size() == 1;

        System.out.println(admin.getReportedReviews(0, Constants.getDefPagLim()));
        assert admin.getReportedReviews(0, Constants.getDefPagLim()).size() == 2: "Incorrect reported reviews size";
        assert admin.evaluateReportedReview(3, true);
        assert admin.getReportedReviews(0, Constants.getDefPagLim()).size() == 1: "Incorrect reported reviews size after evaluation";

    }

}
