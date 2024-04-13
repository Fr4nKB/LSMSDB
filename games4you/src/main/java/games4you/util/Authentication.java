package games4you.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class Authentication {

    private static String salt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[Constants.getSaltLen()];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String hashAndSalt(String password) {
        String salt = salt();
        String hash = hash(password + salt);
        return String.format("%s|%s", hash, salt);
    }

    public static boolean verifyHash(String hashAndSalt, String password) {
        String[] parts = hashAndSalt.split("\\|");
        String salt = parts[1];
        String computedHash = hash(password + salt);

        return MessageDigest.isEqual(computedHash.getBytes(), parts[0].getBytes());
    }

    public static long generateUUID() {
        long maxJavaScriptSafeInt = (long) Math.pow(2, 53) - 1;
        long random = UUID.randomUUID().getMostSignificantBits();
        long safeRandom = random & maxJavaScriptSafeInt;

        if(random < 0 ) return -safeRandom;
        else return safeRandom;
    }

    public static boolean isName(String str) {
        if (str == null || str.length() >= Constants.getMaxSanitizLen() || str.isEmpty()) {
            return false;
        }
        return str.matches(Constants.getNamePattern());
    }

    public static boolean isPassword(String str) {
        if (str == null || str.length() >= Constants.getMaxSanitizLen() || str.length() < 4) {
            return false;
        }

        return str.matches(Constants.getPwdPatter());
    }

    public static boolean isDate(String str) {
        if (str == null || str.length() != 10) return false;
        return str.matches(Constants.getDatePattern());
    }

}