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

        pop.populateUsers();
        pop.populateGames();
        pop.populateReviews();

        assert user.login("JohnnyTheDark", "trustNo1") == 0: "Login failed";

        assert user.addFriend("JohnnyTheDark", "Mary420"): "Friend 1 not added";
        assert user.addFriend("JohnnyTheDark", "XX_ivan_XX"): "Friend 2 not added";

        assert user.getFriendList("JohnnyTheDark", 0).size() == 2: "Incorrect friend list size";
        assert user.removeFriend("JohnnyTheDark", "Mary420");
        assert user.getFriendList("JohnnyTheDark", 0).size() == 1: "Incorrect friend list size after removing one";

        assert user.getGameList("JohnnyTheDark", 0).size() == 2: "Incorrect game list size";

        assert user.getReviewList("AC2", 0).size() == 1: "Incorrect review list size";
        assert user.removeReview("AC2|||JohnnyTheDark");
        assert user.getReviewList("AC2", 0).isEmpty(): "Incorrect review list size after removing one";

    }

}
