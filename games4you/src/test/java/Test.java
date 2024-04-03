import games4you.Admin;
import games4you.User;
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
        User user = new User();
        Admin admin = new Admin();

        assert pop.populateUsers() == 4;
        pop.populateGames();
        pop.populateReviews();

        //LOGIN
        assert user.login("JohnnyTheDark", "trustNo1") == 0: "Login failed";

        //FRIENDS
        assert user.addFriend("JohnnyTheDark", "Mary420"): "Friend 1 not added";
        assert user.addFriend("JohnnyTheDark", "XX_ivan_XX"): "Friend 2 not added";
        assert user.addFriend("XX_ivan_XX", "Parmalat"): "Friend 3 not added";

        assert user.getFriendList("JohnnyTheDark", 0).size() == 2: "Incorrect friend list size";
        assert user.removeFriend("JohnnyTheDark", "Mary420"): "Friend 1 not removed";
        assert user.getFriendList("JohnnyTheDark", 0).size() == 1: "Incorrect friend list size after removing one";

        assert user.addFriend("JohnnyTheDark", "Mary420"): "Friend 1 couldn't be added again";

        //GAMES
        assert user.getGameList("JohnnyTheDark", 0).size() == 2: "Incorrect game list size";

        //REVIEWS
        assert user.getReviewList("AC2", 0).size() == 1: "Incorrect review list size";
        assert user.removeReview("AC2|JohnnyTheDark");
        assert user.getReviewList("AC2", 0).isEmpty(): "Incorrect review list size after removing one";

        //COMPLEX QUERIES
        assert user.tagsRecommendationNORED("JohnnyTheDark").size() == user.tagsRecommendationRED("JohnnyTheDark").size(): "Incorrect recommendation list size";

        //ADMIN
        admin.banUser("JohnnyTheDark");
        assert user.getFriendList("Mary420", 0).isEmpty();
        assert pop.populateUsers() == 0;
        assert user.addReview(admin.publishReview("AC2|Parmalat", false)) == -1;
        assert user.addReview(admin.publishReview("RetroArch|XX_ivan_XX", true)) == 1;

        admin.removeGame("CS2");
        admin.removeGame("CS2");
        assert user.getReviewList("CS2", 0).isEmpty();
        assert user.getGameList("Mary420", 0).size() == 1;

        assert !user.reportReview("Mary420", "Cyberpunk 2077|Mary420");     //cannot report itself
        assert user.reportReview("Mary420", "Cyberpunk 2077|XX_ivan_XX");
        assert user.reportReview("Parmalat", "Cyberpunk 2077|Mary420");
        assert !user.reportReview("Parmalat", "Cyberpunk 2077|Mary420");    //cannot report twice

        user.updatePlayedHours("Mary420", "Cyberpunk 2077", 100);

        System.out.println(admin.getReportedReviews(0));

    }

}
