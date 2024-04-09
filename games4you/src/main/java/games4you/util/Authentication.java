package games4you.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class Authentication {

    private static final int SALT_LEN = 16;
    private static final int MAX_SANITIZATION_LEN = 256;

    // Alphanumeric characters and underscores
    private static final String UNAME_PATTERN = "[a-zA-Z0-9_]*$";

    // Allow alphanumeric characters and some special ones
    private static final String PWD_PATTERN = "^[a-zA-Z0-9!\"#$%&'()*+,-./:;<=>?@\\[\\]^_{}|~]+$";

    // DD/MM/YYYY type of format
    private static final String DATE_PATTERN = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/((19|20)\\d\\d)$";


    private static String salt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[SALT_LEN];
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
        return UUID.randomUUID().getMostSignificantBits();
    }

    public static boolean isUsername(String str) {
        if (str == null || str.length() >= MAX_SANITIZATION_LEN || str.isEmpty()) {
            return false;
        }
        return str.matches(UNAME_PATTERN);
    }

    public static boolean isPassword(String str) {
        if (str == null || str.length() >= MAX_SANITIZATION_LEN || str.length() < 4) {
            return false;
        }

        return str.matches(PWD_PATTERN);
    }

    public static boolean isDate(String str) {
        if (str == null || str.length() != 10) return false;
        return str.matches(DATE_PATTERN);
    }

}