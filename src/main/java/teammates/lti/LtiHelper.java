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
    public static String key = "7488e962c2a330543e71a26d0a17bd24";
    /**
     * Secret.
     */
    public static String secret = "d072fa6998fd7d4282cd14892f65d24b";

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
