package games4you.webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.util.Authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;

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

    @GetMapping("/home/user/friends")
    public ArrayList<Object> homePageLoadMoreFriends(@RequestParam("offset") int offset, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.homePageFriends(ret[0], offset);
    }

    @GetMapping("/home/user/reviews")
    public ArrayList<Object> homePageLoadMoreReviews(@RequestParam("offset") int offset, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;
        return gamerMethods.homePageReviews(ret[0], offset);
    }

}