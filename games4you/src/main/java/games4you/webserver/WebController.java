package games4you.webserver;

import games4you.entities.Admin;
import games4you.entities.Gamer;
import games4you.webserver.SessionManager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.tags.ArgumentAware;

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

    /**
     * Retrieves access token and validates it
     * @param request to request cookies
     * @return -1 if token not found or invalid, 0 if user is normal or 1 if is admin
     */
    public int[] isUserAdmin(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    return sesManager.validateToken(cookie.getValue());
                }
            }
        }

        return null;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(String uname, String pwd, HttpServletRequest request, HttpServletResponse response) {
        //first check if any cookie is present
        int[] ret = isUserAdmin(request);
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
        formData.put("uid", 69);        //TODO: use UUIDs
        formData.put("isAdmin", false);
        formData.put("tags", new ArrayList<>());
        if(gamerMethods.signup(formData)) return "redirect:/login";
        return "redirect:/signup";
    }

    @GetMapping("/home")
    public ModelAndView homeUser(HttpServletRequest request) {
        ModelAndView mod = new ModelAndView("home");
        int[] ret = isUserAdmin(request);
        if(ret[1] == 0) {
            ArrayList<Object> content = gamerMethods.homePage(ret[1], 0);
            mod.addObject("jsonList", content);
        }
        else if(ret[1] == 1) {
            ArrayList<Object> content = adminMethods.getReportedReviews(0);
            mod.addObject("jsonList", content);
        }

        return mod;
    }
}