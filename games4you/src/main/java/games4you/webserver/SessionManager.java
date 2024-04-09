package games4you.webserver;

import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static SecretKey secKey;
    private static Map<String, Long> tokenStore;

    public SessionManager() {
         secKey = Jwts.SIG.HS256.key().build();
         tokenStore = new HashMap<>();      //O(1) to check existence of tokens
    }

    public static String generateToken(long uid, long isAdmin) {

        // Generate a date which is 3 days after today
        Date currDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currDate);
        calendar.add(Calendar.DATE, 3);
        Date expDate = calendar.getTime();

        String token = Jwts.builder()
                .subject(Long.toString(uid))
                .claim("isAdmin", isAdmin)
                .issuedAt(currDate)
                .expiration(expDate)
                .signWith(secKey)
                .compact();

        tokenStore.put(token, uid);

        return token;
    }

    public static long[] validateToken(String token) {
        try {
            if (!tokenStore.containsKey(token)) return null;  // Check if the token exists

            Claims claims = Jwts.parser().verifyWith(secKey).build().parseSignedClaims(token).getPayload();

            if (claims.getExpiration().before(new Date())) return null;   // Check if the token is expired

            // return if user is admin or not
            long[] ret = new long[2];
            ret[0] = Long.parseLong(claims.getSubject());
            ret[1] = ((Integer) claims.get("isAdmin")).longValue();
            return ret;

        }
        catch (JwtException e) {
            return null;
        }
    }

    /**
     * Retrieves access token and validates it
     * @param request to request cookies
     * @return -1 if token not found or invalid, 0 if user is normal or 1 if is admin
     */
    public long[] isUserAdmin(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    return validateToken(cookie.getValue());
                }
            }
        }

        return null;
    }

    public boolean removeSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies == null) return false;

        for (Cookie cookie: cookies) {
            if (cookie.getName().equals("token")) {
                tokenStore.remove(cookie.getValue());
                return true;
            }
        }

        return false;
    }
}