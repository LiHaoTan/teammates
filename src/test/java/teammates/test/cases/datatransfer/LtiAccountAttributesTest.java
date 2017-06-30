package teammates.test.cases.datatransfer;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.LtiAccountAttributes;
import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.test.cases.BaseTestCase;

/**
 * SUT: {@link LtiAccountAttributes}.
 */
public class LtiAccountAttributesTest extends BaseTestCase {

    //TODO: add test for constructor

    @Test
    public void testValidate() throws Exception {
        // TODO?
    }

    @Test
    public void testGetValidityInfo() {
        //already tested in testValidate() above
    }

    @Test
    public void testIsValid() {
        //already tested in testValidate() above
    }

    @Test
    public void testToString() {
        LtiAccountAttributes ltiAccountAttributes = generateValidLtiOAuthAttributesObject();
        assertEquals(
                "{\n"
                        + "  \"userId\": \"valid-id-$_abc\",\n"
                        + "  \"regkey\": \"regkey\",\n"
                        + "  \"googleId\": \"googleId@gmail.com\"\n"
                        + "}",
                ltiAccountAttributes.toString());
    }

    @Test
    public void testGetBackupIdentifier() {
        assertEquals("LtiAccount", generateValidLtiOAuthAttributesObject().getBackupIdentifier());
    }


    private static LtiAccountAttributes generateValidLtiOAuthAttributesObject() {
        return new LtiAccountAttributes("valid-id-$_abc", "regkey", "googleId@gmail.com");
    }

}
