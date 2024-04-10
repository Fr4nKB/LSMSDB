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

}