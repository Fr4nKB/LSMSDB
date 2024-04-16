package webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.util.Populator;

import java.io.IOException;

public class Populate {
    public static void main(String[] args) {

        Populator pop = new Populator();
        Gamer gamer = new Gamer();

        assert pop.populateConcurrent("userDB.json", 0) == 5;
        pop.populateConcurrent("old_gameDB.json", 1);
        pop.populateConcurrent("old_reviewDB.json", 0);

        //FRIENDS
        assert gamer.sendRequest(0, 1): "Friend 1 not sent";
        assert gamer.acceptRequest(1, 0): "Friend 1 not added";
        assert gamer.sendRequest(0, 2): "Friend 2 not sent";
        assert gamer.acceptRequest(2, 0): "Friend 2 not added";
        assert gamer.sendRequest(1, 2): "Friend 3 not sent";
        assert gamer.acceptRequest(2, 1): "Friend 3 not added";
        assert gamer.sendRequest(2, 3): "Friend 4 not sent";
        assert gamer.sendRequest(1, 3): "Friend 3 not sent";
        //assert gamer.acceptRequest(3, 2): "Friend 4 not added";
        assert gamer.sendRequest(3, 0): "Friend 5 not sent";
        //assert gamer.acceptRequest(0, 3): "Friend 5 not added";

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

        assert !gamer.upvoteReview(0, 0);
        assert gamer.upvoteReview(0, 3);
        assert gamer.upvoteReview(2, 3);
        assert !gamer.upvoteReview(2, 3);

    }

}
