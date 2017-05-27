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
    public static String key = "9df8eaf52957c350b66ae9ac700b76ec";
    /**
     * Secret.
     */
    public static String secret = "d1aa1b80f20b79608c56dbeb48cb2901";

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
