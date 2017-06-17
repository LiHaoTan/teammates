package teammates.test.cases.datatransfer;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.test.cases.BaseTestCase;

/**
 * SUT: {@link LtiOAuthCredentialAttributes}.
 */
public class LtiOAuthCredentialAttributesTest extends BaseTestCase {

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
        LtiOAuthCredentialAttributes ltiOAuthCredentialAttributes = generateValidLtiOAuthAttributesObject();
        assertEquals(
                "{\n"
                        + "  \"consumerKey\": \"valid-id-$_abc\",\n"
                        + "  \"consumerSecret\": \"toCreateAValidKey\"\n"
                        + "}",
                ltiOAuthCredentialAttributes.toString());
    }

    private static LtiOAuthCredentialAttributes generateValidLtiOAuthAttributesObject() {
        return new LtiOAuthCredentialAttributes("valid-id-$_abc", "toCreateAValidKey");
    }

}
