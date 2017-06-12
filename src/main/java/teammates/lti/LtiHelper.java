package teammates.lti;

import java.security.SecureRandom;

/**
 * Test.
 */
public class LtiHelper {
    /**
     * Key.
     */
    //public static String secret = generateSecret();
    public static String key = "255d4a7bfa0eccc8bd3ef59c5d20ca59783cd5ec";
    /**
     * Secret.
     */
    public static String secret = "c0137f3a895fad1ef38ddda89727e8305d77347a";

    private static SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate secret temp.
     */
    public static String generateSecret() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return new String(bytes);
    }

}
