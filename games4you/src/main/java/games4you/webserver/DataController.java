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
        ArrayList<Object> r = gamerMethods.getUserReviewList(uid, offset);
        System.out.println(r);
        return r;
    }

    @GetMapping("/addFriend/{id}")
    public int addFriend(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return -1;   //admins can't have friends

        return gamerMethods.addFriend(ret[0], uid);
    }

    @GetMapping("/removeFriend/{id}")
    public int removeFriend(@PathVariable("id") long uid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return -1;   //admins can't remove friends, they don't have any

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

}