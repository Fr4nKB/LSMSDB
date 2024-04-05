import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.util.Populator;

import java.io.IOException;

public class Test {
    public void main() {

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

        assert pop.populateGamers() == 4;
        assert pop.populateGamers() == 0: "Users already present";
        pop.populateGames();
        pop.populateReviews();

        //LOGIN
        assert gamer.login("JohnnyTheDark", "trustNo1") == 0: "Login failed";

        //FRIENDS
        assert gamer.addFriend(0, 1): "Friend 1 not added";
        assert gamer.addFriend(0, 2): "Friend 2 not added";
        assert gamer.addFriend(1, 2): "Friend 3 not added";
        assert gamer.addFriend(2, 3): "Friend 4 not added";
        assert gamer.addFriend(3, 0): "Friend 4 not added";
        System.out.println(gamer.homePage(0, 0));
        assert gamer.homePage(0, 0).size() == 7: "Incorrect home page";


        System.out.println(gamer.getFriendList(0, 0));
        assert gamer.getFriendList(0, 0).size() == 2: "Incorrect friend list size";
        assert gamer.removeFriend(0, 1): "Friend 1 not removed";
        assert gamer.getFriendList(0, 0).size() == 1: "Incorrect friend list size after removing one";

        assert gamer.addFriend(0, 1): "Friend 1 couldn't be added again";

        //GAMES
        System.out.println(gamer.getGameList(0,0));
        assert gamer.getGameList(0, 0).size() == 2: "Incorrect game list size";

        //REVIEWS
        System.out.println(gamer.getReviewList(1, 0));
        assert gamer.getReviewList(0, 0).size() == 2: "Incorrect review list size";
        assert gamer.removeReview(0);
        assert gamer.getReviewList(0, 0).size() == 1: "Incorrect review list size after removing one";

        //COMPLEX QUERIES
        assert gamer.tagsRecommendationNORED(0).size() == gamer.tagsRecommendationRED(0).size(): "Incorrect recommendation list size";

        //ADMIN
        assert admin.banGamer(0): "User not banned";
        assert gamer.getFriendList(1, 0).size() == 1;
        assert admin.removeGame(1);
        assert !admin.removeGame(1): "Game was already removed";
        assert gamer.getReviewList(1, 0).isEmpty();
        assert gamer.getGameList(1, 0).size() == 1;

        assert !gamer.reportReview(1, 3);     //cannot report itself
        assert gamer.reportReview(1, 4);
        assert gamer.reportReview(4, 3);
        assert !gamer.reportReview(4, 3);    //cannot report twice

        gamer.updatePlayedHours(1, 3, 100);

        System.out.println(admin.getReportedReviews(0));
        assert admin.getReportedReviews(0).size() == 2: "Incorrect reported reviews size";
        assert admin.evaluateReportedReview(3, true);
        assert admin.getReportedReviews(0).size() == 1: "Incorrect reported reviews size after evaluation";

    }

}
