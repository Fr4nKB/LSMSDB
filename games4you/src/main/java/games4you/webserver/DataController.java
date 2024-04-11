package games4you.webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;

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

        if(ret[1] == 1) return adminMethods.getReportedReviews(offset);
        else if(ret[1] == 0) return gamerMethods.homePage(ret[0], offset);
        else return null;
    }

    @GetMapping("/search/users/{user}")
    public ArrayList<Object> searchMoreUsers(@PathVariable("user") String user,
                                             @RequestParam("offset") int offset,
                                             HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.browseUsers(user, offset);
    }

    @GetMapping("/user/reviews/")
    public ArrayList<Object> loadMoreUserReviews(@RequestParam("uid") long uid,
                                             @RequestParam("offset") int offset,
                                             HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.getUserReviewList(uid, offset);
    }

    @GetMapping("/checkFriendship/{id}")
    public String checkFriend(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;   //admins can't have friends

        return gamerMethods.checkFriendshipStatus(ret[0], uid);
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
                                            HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        return gamerMethods.getFriendList(uid, offset);
    }

    @GetMapping("/search/games/{id}/more")
    public ArrayList<Object> gameListMore(@PathVariable("id") long uid, @RequestParam("offset") int offset,
                                          HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        return gamerMethods.getGameList(uid, offset);
    }

    @GetMapping("/upvote/{id}")
    public boolean upvoteReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return false;

        return gamerMethods.upvoteReview(rid);
    }

}