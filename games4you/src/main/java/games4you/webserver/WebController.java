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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    @GetMapping("/")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(String uname, String pwd, HttpServletRequest request, HttpServletResponse response) {
        //first check if any cookie is present
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) { // no cookies, check user credentials
            ret = gamerMethods.login(uname, pwd);
            if(ret == null) return "redirect:/";

            // generate and add access token to cookies
            String token = sesManager.generateToken(ret[0], ret[1]);
            Cookie cookie = new Cookie("token", token);
            response.addCookie(cookie);
        }

        if(ret[1] == 0) return "redirect:/home";
        else if(ret[1] == 1) return "redirect:/home";

        return "redirect:/";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String postSignup(@RequestParam HashMap<String, Object> formData) {
        formData.put("uid", Authentication.generateUUID());
        formData.put("isAdmin", false);
        formData.put("datecreation", Instant.now().getEpochSecond());
        if(gamerMethods.signup(formData)) return "redirect:/";
        return "redirect:/signup";
    }

    @GetMapping("/home")
    public String homeUser(HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);

        if(ret == null) return "redirect:/error";
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
        if(ret[1] == 1) mod.addObject("adm", true);
        else if(ret[1] == 0) mod.addObject("uid", ret[0]);
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
        if(ret[1] == 1) mod.addObject("adm", true);
        mod.addObject("jsonData", json);
        return mod;
    }

    @GetMapping("/review/{id}")
    public ModelAndView getReview(@PathVariable("id") long rid, HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        String json = gamerMethods.showReview(rid);
        System.out.println(json);
        if(json == null) return null;

        ModelAndView mod = new ModelAndView("review");
        if(ret[1] == 1) mod.addObject("adm", true);
        else if(ret[1] == 0) mod.addObject("uid", ret[0]);
        mod.addObject("rid", rid);
        mod.addObject("jsonString", json);
        return mod;
    }

    @GetMapping("/search")
    public ModelAndView search(@RequestParam("type") String type, @RequestParam("query") String query,
                               HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("search");

        if(!Authentication.isName(query)) return null;
        String[] parts = type.split("-");
        if(parts.length > 2) return null;
        else if(parts.length == 2 && (!Authentication.isName(parts[0]) || !Authentication.isName(parts[1]))) return null;

        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        if(parts.length == 2) mod.addObject("endpoint", parts[0] + "/" + parts[1] + "/" + query);
        else mod.addObject("endpoint", parts[0] + "/" + query);
        return mod;
    }

    @GetMapping("/search/friends/{id}")
    public ModelAndView friendList(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("search");

        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        mod.addObject("endpoint", String.format("friends/%d/more", id));
        return mod;
    }

    @GetMapping("/search/games/{id}")
    public ModelAndView gameList(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("search");

        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null) return null;

        mod.addObject("endpoint", String.format("games/%d/more", id));
        return mod;
    }

    @GetMapping("/newReview/{id}")
    public ModelAndView newReview(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("newReview");

        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;     // admins cannot write reviews

        mod.addObject("gid", id);
        return mod;
    }

    @PostMapping("/newReview/{id}")
    public String postNewReview(@PathVariable("id") long gid, @RequestParam HashMap<String, Object> formData,
                                HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 1) return null;     // admins cannot write reviews

        String rating = (String) formData.get("rating");
        if(rating.equals("on")) formData.replace("rating", rating, true);
        else formData.replace("rating", rating, false);

        formData.remove("gid");
        formData.put("gid", gid);

        long rid = Authentication.generateUUID();
        formData.put("rid", rid);
        formData.put("uid", ret[0]);
        formData.put("creation_date", Instant.now().getEpochSecond());

        String uname = gamerMethods.retrieveUname(ret[0]);
        if(uname == null) return "redirect:/error";
        String game = gamerMethods.retrieveGameName(gid);
        if(game == null) return "redirect:/error";

        formData.put("username", uname);
        formData.put("game", game);

        if(gamerMethods.addReview(formData) <= 0) return "redirect:/error";
        else return String.format("redirect:/review/%d", rid);
    }


    @GetMapping("/newGame/")
    public String newGame(HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 0) return "redirect:/error";     // gamers cannot create new games
        return "newGame";
    }

    @PostMapping("/newGame/")
    public String postNewGame(@RequestParam HashMap<String, Object> formData,
                                HttpServletRequest request) {
        long[] ret = sesManager.isUserAdmin(request);
        if(ret == null || ret[1] == 0) return null;     // gamers cannot create new games

        long gid = Authentication.generateUUID();
        formData.put("gid", gid);

        try {
            String tags_field = (String) formData.get("tags");
            String[] tags = tags_field.split(", ");
            ArrayList<String> tagsArr = new ArrayList<>();
            Collections.addAll(tagsArr, tags);
            formData.replace("tags", tags_field, tagsArr);

            String date = (String) formData.get("release_date");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(date, formatter);
            int seconds = (int) localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            formData.replace("release_date", date, seconds);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "redirect:/error";
        }

        if(adminMethods.insertGame(formData)) return String.format("redirect:/game/%d", gid);
        else return "redirect:/error";
    }

}