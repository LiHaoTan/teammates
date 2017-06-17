package teammates.ui.controller;

import java.security.SecureRandom;

import com.google.common.io.BaseEncoding;

import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.Logger;
import teammates.storage.api.LtiOAuthCredentialDb;
import teammates.ui.pagedata.AdminLtiCredentialsPageData;

public class AdminLtiCredentialsGenerateAction extends Action {

    private static final Logger log = Logger.getLogger();

    @Override
    protected ActionResult execute() {

        gateKeeper.verifyAdminPrivileges(account);

        AdminLtiCredentialsPageData data = new AdminLtiCredentialsPageData(account, sessionToken);

        //data.isInstructorAddingResultForAjax = true;
        //data.statusForAjax = createInstructorCreationSuccessAjaxStatus(data, joinLink);

        LtiOAuthCredentialDb ltiOAuthCredentialDb = new LtiOAuthCredentialDb();
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String generatedSecret = BaseEncoding.base16().lowerCase().encode(bytes);
        try {
            String consumerKey = getNonNullRequestParamValue(Const.ParamsNames.LTI_CREDENTIALS_CONSUMER_KEY);
            ltiOAuthCredentialDb.createCredential(
                    new LtiOAuthCredentialAttributes(consumerKey, generatedSecret));
            data.consumerKey = consumerKey;
            data.consumerSecret = generatedSecret;
            data.isInstructorAddingResultForAjax = true;
            data.statusForAjax = "Successfully generated credentials";
        } catch (InvalidParametersException e) {
            data.isInstructorAddingResultForAjax = false;
            data.statusForAjax = e.getMessage();
        }

        return createAjaxResult(data);
    }
}
