package games4you.webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;

import games4you.util.Constants;
import games4you.util.NeoComplexQueries;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
public class DataController {

    private final Admin adminMethods;
    private final Gamer gamerMethods;
    private final SessionManager sesManager;

    public DataController() {
        adminMethods = new Admin();
        gamerMethods = new Gamer();
        sesManager = new SessionManager();
    }

    @GetMapping("/logout")
    public boolean logout(HttpServletRequest request) {
        if(!sesManager.removeSession(request)) return false;
        else return true;
    }

    @GetMapping("/home/more")
    public ArrayList<Object> homePageLoadMore(@RequestParam("offset") int offset, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        if(ret[1] == 1) return adminMethods.getReportedReviews(offset, Constants.getDefPagLim());
        else if(ret[1] == 0) return gamerMethods.homePage(ret[0], offset, Constants.getDefPagLim());
        else return null;
    }

    @GetMapping("/search/users/{user}")
    public ArrayList<Object> searchMoreUsers(@PathVariable("user") String user,
                                             @RequestParam("offset") int offset,
                                             HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.browseUsers(user, offset, 20);
    }

    @GetMapping("/search/games/name/{game}")
    public ArrayList<Object> searchMoreGamesByName(@PathVariable("game") String game,
                                             @RequestParam("offset") int offset,
                                             HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.browseGamesByName(game, offset, Constants.getDefPagLim());
    }

    @GetMapping("/search/games/tags/{tag}")
    public ArrayList<Object> searchMoreGamesByTags(@PathVariable("tag") String tag,
                                                   @RequestParam("offset") int offset,
                                                   HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.browseGamesByTags(tag, offset, Constants.getDefPagLim());
    }

    @GetMapping("/user/reviews/")
    public ArrayList<Object> loadMoreUserReviews(@RequestParam("uid") long uid,
                                             @RequestParam("offset") int offset,
                                             HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.getUserReviewList(uid, offset);
    }

    @GetMapping("/game/reviews/")
    public ArrayList<Object> loadMoreGameReviews(@RequestParam("gid") long gid,
                                                 @RequestParam("offset") int offset,
                                                 HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.getGameReviewList(gid, offset);
    }

    @GetMapping("/checkFriendship/{id}")
    public String checkFriend(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;   //admins can't have friends

        return gamerMethods.checkFriendshipStatus(ret[0], uid);
    }

    @GetMapping("/checkGame/{id}")
    public String checkGame(@PathVariable("id") long gid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;   //admins can't have games

        return gamerMethods.checkGameRelationship(ret[0], gid);
    }


    @GetMapping("/checkReview/{id}")
    public String checkReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;   //admins can't have games

        return gamerMethods.checkReviewRelationship(ret[0], rid);
    }


    @GetMapping("/sendRequest/{id}")
    public boolean sendRequest(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;   //admins can't have friends

        return gamerMethods.sendRequest(ret[0], uid);
    }

    @GetMapping("/revokeRequest/{id}")
    public boolean revokeRequest(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;   //admins can't have friends

        return gamerMethods.revokeRequest(ret[0], uid);
    }

    @GetMapping("/acceptRequest/{id}")
    public boolean acceptRequest(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;   //admins can't have friends

        return gamerMethods.acceptRequest(ret[0], uid);
    }


    @GetMapping("/declineRequest/{id}")
    public boolean declineRequest(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;   //admins can't have friends

        return gamerMethods.declineRequest(ret[0], uid);
    }

    @GetMapping("/getFriendRequest/")
    public ArrayList<Object> getFriendRequest(@RequestParam("offset") int offset,
                                              HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;   //admins can't have friends

        return gamerMethods.getRequestsList(ret[0], offset, 1);
    }

    @GetMapping("/removeFriend/{id}")
    public boolean removeFriend(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;   //admins can't remove friends, they don't have any

        return gamerMethods.removeFriend(ret[0], uid);
    }

    @GetMapping("/ban/{id}")
    public boolean ban(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 0) return false;   //gamers cannot ban other gamers

        return adminMethods.banGamer(uid);
    }

    @GetMapping("/addGame/{id}")
    public int addGame(@PathVariable("id") long gid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return -1;   //admins can't have friends

        return gamerMethods.addGameToLibrary(ret[0], gid);
    }

    @GetMapping("/removeGame/{id}")
    public int removeGame(@PathVariable("id") long gid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return -1;   //admins can't have friends

        return gamerMethods.removeGameFromLibrary(ret[0], gid);
    }

    @GetMapping("/deleteGame/{id}")
    public boolean banGame(@PathVariable("id") long gid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 0) return false;   //gamers cannot delete games

        return adminMethods.deleteGame(gid);
    }

    @GetMapping("/search/friends/{id}/more")
    public ArrayList<Object> friendListMore(@PathVariable("id") long uid, @RequestParam("offset") int offset,
                                            @RequestParam("limit") int limit, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        int lim;
        if(limit != -1) lim = limit;
        else lim = Constants.getDefPagLim();

        return gamerMethods.getFriendList(uid, offset, lim);
    }

    @GetMapping("/search/games/{id}/more")
    public ArrayList<Object> gameListMore(@PathVariable("id") long uid, @RequestParam("offset") int offset,
                                          @RequestParam("limit") int limit, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        int lim;
        if(limit != -1) lim = limit;
        else lim = Constants.getDefPagLim();

        return gamerMethods.getGameList(uid, offset, lim);
    }



    @GetMapping("/reportReview/{id}")
    public boolean reportReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;

        return gamerMethods.reportReview(ret[0], rid);
    }

    @GetMapping("/upvoteReview/{id}")
    public boolean upvoteReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;

        return gamerMethods.upvoteReview(ret[0], rid);
    }

    @GetMapping("/removeReview/{id}")
    public boolean removeReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;

        return gamerMethods.removeReview(rid);
    }

    @PostMapping("/updateHours/{id}")
    public boolean updateHours(@PathVariable("id") long gid, @RequestParam("hours") int hours, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;    // admins don't have games

        return gamerMethods.updatePlayedHours(ret[0], gid, hours);
    }

    @GetMapping("/evaluateReview")
    public boolean evaluateReview(@RequestParam("rid") long rid, @RequestParam("judgment") boolean judgment,
                               HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 0) return false;    // gamers cannot evaluate reported reviews

        return adminMethods.evaluateReportedReview(rid, judgment);
    }
}