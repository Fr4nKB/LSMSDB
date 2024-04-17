package games4you.util;

public class Constants {
    private static final int MAX_PAGINATION_LIMIT = 100;

    private static final int DEFAULT_PAGINATION_LEN = 20;

    private static final int SALT_LEN = 16;

    // uname and pwds longer than this are refused
    private static final int MAX_SANITIZATION_LEN = 64;

    // Alphanumeric characters and underscores
    private static final String NAME_PATTERN = "[a-zA-Z0-9 _+.-]*$";

    // Allow alphanumeric characters and some special ones
    private static final String PWD_PATTERN = "^[a-zA-Z0-9!\"#$%&'()*+,-./:;<=>?@\\[\\]^_{}|~]+$";

    // DD/MM/YYYY type of format
    private static final String DATE_PATTERN = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/((19|20)\\d\\d)$";

    public static int getMaxPagLim() {
        return MAX_PAGINATION_LIMIT;
    }

    public static int getDefPagLim() {
        return DEFAULT_PAGINATION_LEN;
    }

    public static int getSaltLen() {
        return SALT_LEN;
    }

    public static int getMaxSanitizLen() {
        return MAX_SANITIZATION_LEN;
    }

    public static String getNamePattern() {
        return NAME_PATTERN;
    }

    public static String getPwdPatter() {
        return PWD_PATTERN;
    }

    public static String getDatePattern() {
        return DATE_PATTERN;
    }

}
