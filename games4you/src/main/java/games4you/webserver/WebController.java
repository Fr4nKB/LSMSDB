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

@Controller
public class WebController {

    private final Admin adminMethods;
    private final Gamer gamerMethods;
    private final SessionManager sesManager;

    public WebController() {
        adminMethods = new Admin();
        gamerMethods = new Gamer();
        sesManager = new SessionManager();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(String uname, String pwd, HttpServletRequest request, HttpServletResponse response) {
        //first check if any cookie is present
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) { // no cookies, check user credentials
            ret = gamerMethods.login(uname, pwd);
            if(ret == null) return "redirect:/login";

            // generate and add access token to cookies
            String token = sesManager.generateToken(ret[0], ret[1]);
            Cookie cookie = new Cookie("token", token);
            response.addCookie(cookie);
        }

        if(ret[1] == 0) return "redirect:/home";
        else if(ret[1] == 1) return "redirect:/home";

        return "redirect:/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String postSignup(@RequestParam HashMap<String, Object> formData) {
        formData.put("uid", Authentication.generateUUID());
        formData.put("isAdmin", false);
        formData.put("tags", new ArrayList<>());
        if(gamerMethods.signup(formData)) return "redirect:/login";
        return "redirect:/signup";
    }

    @GetMapping("/home")
    public String homeUser(HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);

        if(ret == null) return "error";
        else if(ret[1] == 1) return "homeAdmin";
        else return "home";
    }

    @GetMapping("/user/{id}")
    public ModelAndView getUser(@PathVariable("id") String id, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        long uid;
        try {
            uid = Long.parseLong(id);
        }
        catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }

        String json = gamerMethods.showUser(uid);
        if(json == null) return null;

        ModelAndView mod = new ModelAndView("user");
        mod.addObject("jsonData", json);
        return mod;
    }

    @GetMapping("/game/{id}")
    public ModelAndView getGame(@PathVariable("id") String id, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        long gid;
        try {
            gid = Long.parseLong(id);
        }
        catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }

        String json = gamerMethods.showGame(gid);
        if(json == null) return null;

        ModelAndView mod = new ModelAndView("game");
        mod.addObject("jsonData", json);
        return mod;
    }

    @GetMapping("/review/{id}")
    public ModelAndView getReview(@PathVariable("id") String id, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        long rid;
        try {
            rid = Long.parseLong(id);
        }
        catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }

        String json = gamerMethods.showReview(rid);
        if(json == null) return null;

        ModelAndView mod = new ModelAndView("review");
        if(ret[1] == 1) mod.addObject("uid", null);
        else if(ret[1] == 0) mod.addObject("uid", ret[0]);
        mod.addObject("jsonString", json);
        return mod;
    }

    @GetMapping("/search")
    public ModelAndView search(@RequestParam("type") String type, @RequestParam("query") String query,
                               HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("search");

        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        mod.addObject("type", type);
        mod.addObject("query", query);
        return mod;
    }

}