package games4you.webserver;

import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static SecretKey secKey;
    private static final Map<String, Integer> tokenStore = new HashMap<>();      //O(1) to check existence of tokens

    public SessionManager() {
         secKey = Jwts.SIG.HS256.key().build();
    }

    public static String generateToken(int uid, int isAdmin) {

        // Generate a date which is 3 days after today
        Date currDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currDate);
        calendar.add(Calendar.DATE, 3);
        Date expDate = calendar.getTime();

        String token = Jwts.builder()
                .subject(Integer.toString(uid))
                .claim("isAdmin", isAdmin)
                .issuedAt(currDate)
                .expiration(expDate)
                .signWith(secKey)
                .compact();

        tokenStore.put(token, uid);

        return token;
    }

    public static int[] validateToken(String token) {
        try {
            if (!tokenStore.containsKey(token)) return null;  // Check if the token exists

            Claims claims = Jwts.parser().verifyWith(secKey).build().parseSignedClaims(token).getPayload();

            if (claims.getExpiration().before(new Date())) return null;   // Check if the token is expired

            // return if user is admin or not
            int[] ret = new int[2];
            ret[0] = Integer.parseInt(claims.getSubject());
            ret[1] = (int) claims.get("isAdmin");
            return ret;

        }
        catch (JwtException e) {
            return null;
        }
    }
}