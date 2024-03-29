package games4you.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Authentication {

    private static final int SALT_LEN = 16;
    private static final int MAX_SANITIZATION_LEN = 256;

    private static String generateSalt() {
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

    public static String hashAndSalt(String password, String salt) {
        if (salt.isEmpty()) {
            salt = generateSalt();
        }

        String hash = hash(password + salt);
        return STR."\{hash}|\{salt}";
    }

    public static boolean verifyHash(String hashAndSalt, String password) {
        String[] parts = hashAndSalt.split("\\|");
        String salt = parts[1];
        String computedHash = hash(password + salt);

        return MessageDigest.isEqual(computedHash.getBytes(), parts[0].getBytes());
    }

    public static boolean isUsername(String str) {
        if (str.length() >= MAX_SANITIZATION_LEN || str.isEmpty()) {
            return false;
        }
        // Alphanumeric characters and underscores, starting with a letter
        String pattern = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]*$";
        return str.matches(pattern);
    }

    public static boolean isPassword(String str) {
        if (str.length() >= MAX_SANITIZATION_LEN || str.length() < 4) {
            return false;
        }

        // Allow alphanumeric characters and some special ones
        return str.matches("^[a-zA-Z0-9!\"#$%&'()*+,-./:;<=>?@\\\\[\\\\]^_{}|~]+$");
    }
}