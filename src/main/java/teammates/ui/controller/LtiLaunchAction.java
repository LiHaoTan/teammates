package teammates.ui.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.imsglobal.lti.launch.LtiLaunch;
import org.imsglobal.lti.launch.LtiOauthSigner;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiSigner;
import org.imsglobal.lti.launch.LtiSigningException;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;

import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.LtiAccountAttributes;
import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.LtiRequiredCredentialsNotFoundException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.Logger;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.common.util.Url;
import teammates.logic.core.InstructorsLogic;
import teammates.storage.api.LtiAccountDb;
import teammates.storage.api.LtiOAuthCredentialDb;
import teammates.ui.pagedata.AdminHomePageData;
import teammates.ui.pagedata.PageData;

/**
 * Handles an LTI launch as per the
 * <a href="https://www.imsglobal.org/activity/learning-tools-interoperability" target="_blank">Learning Tools Interoperability specification</a>.
 */
public class LtiLaunchAction extends Action {

    private static final Logger log = Logger.getLogger();

    private static final String LTI_ERRORMSG = "lti_errormsg";
    private static final String LTI_ERRORLOG = "lti_errorlog";

    private static final String CONTEXT_ROLE_INSTRUCTOR_HANDLE = "Instructor";
    private static final String CONTEXT_ROLE_INSTRUCTOR_URN = "urn:lti:role:ims/lis/Instructor";

    private static final String CONTEXT_ROLE_LEARNER_INSTRUCTOR_HANDLE = "Learner/Instructor";
    private static final String CONTEXT_ROLE_LEARNER_INSTRUCTOR_URN = "urn:lti:role:ims/lis/Learner/Instructor";

    private static final String CONTEXT_ROLE_INSTRUCTOR_PRIMARY_INSTRUCTOR_HANDLE = "Instructor/PrimaryInstructor";
    private static final String CONTEXT_ROLE_INSTRUCTOR_PRIMARY_INSTRUCTOR_URN =
            "urn:lti:role:ims/lis/Instructor/PrimaryInstructor";

    private static final String CONTEXT_ROLE_INSTRUCTOR_LECTURER_HANDLE = "Instructor/Lecturer";
    private static final String CONTEXT_ROLE_INSTRUCTOR_LECTURER_URN = "urn:lti:role:ims/lis/Instructor/Lecturer";

    private static final String CONTEXT_ROLE_INSTRUCTOR_GUEST_INSTRUCTOR_HANDLE = "Instructor/GuestInstructor";
    private static final String CONTEXT_ROLE_INSTRUCTOR_GUEST_INSTRUCTOR_URN =
            "urn:lti:role:ims/lis/Instructor/GuestInstructor";

    private static final String CONTEXT_ROLE_INSTRUCTOR_EXTERNAL_INSTRUCTOR_HANDLE = "Instructor/ExternalInstructor";
    private static final String CONTEXT_ROLE_INSTRUCTOR_EXTERNAL_INSTRUCTOR_URN =
            "urn:lti:role:ims/lis/Instructor/ExternalInstructor";

    private static final String INSTITUTION_ROLE_INSTRUCTOR_URN = "urn:lti:instrole:ims/lis/Instructor";

    @Override
    public AccessType getAccessType() {
        return AccessType.LTI;
    }

    @Override
    protected ActionResult execute() {
        logParameters();

        // move all these into gatekeeper
        LtiVerificationResult verificationResult;
        try {
            verificationResult = verifyOAuth(request);
        } catch (LtiRequiredCredentialsNotFoundException | LtiVerificationException e) {
            throw new UnauthorizedAccessException(e.getMessage());
        }

        if (!verificationResult.getSuccess()) {
            log.warning("Invalid request LTI OAuth");
            throw new UnauthorizedAccessException("Invalid request LTI OAuth");
        }
        // end here

        final LtiLaunch ltiLaunchResult = verificationResult.getLtiLaunchResult();
        if (ltiLaunchResult.getUser().getId() == null || ltiLaunchResult.getUser().getRoles().size() == 0) {
            return createCannotIdentifyUserErrorPageWithLogResult(ltiLaunchResult);
        }

        log.info("All roles: " + request.getParameter("roles"));
        log.info("Unique user id: " + ltiLaunchResult.getUser().getId());

        request.getSession().setAttribute("access_type", "lti");

        for (String role : ltiLaunchResult.getUser().getRoles()) {
            log.info("User role: " + role);
            if (isInstructorRole(role)) {
                log.info("INSTRUCTOR FOUND!");
                String userId = ltiLaunchResult.getUser().getId();

                request.getSession().setAttribute("role", "instructor");
                request.getSession().setAttribute("user_id", userId);

                LtiAccountDb ltiAccountDb = new LtiAccountDb();
                LtiAccountAttributes ltiAccountAttributes = ltiAccountDb.getAccount(userId);
                if (ltiAccountAttributes == null) {
                    try {
                        ltiAccountDb.createAccount(new LtiAccountAttributes(userId));
                    } catch (InvalidParametersException e) {
                        log.severe("Can't create LtiAccount");
                        throw new UnauthorizedAccessException("Invalid request LTI OAuth");
                    }
                    return createInstructorAccount(request);
                } else if (ltiAccountAttributes.getGoogleId() != null) {
                    return createRedirectResult(Const.ActionURIs.INSTRUCTOR_HOME_PAGE);
                } else if (ltiAccountAttributes.getRegkey() != null) {
                    return reconfirmInstructorDetails(request, ltiAccountAttributes);
                } else {
                    return createInstructorAccount(request);
                }
            }
        }
        return createNotInstructorErrorPageResult(ltiLaunchResult);
    }

    /**
     * Returns a redirect back to the tool consumer with an error message to be shown appropriately, indicating that only
     * instructors can access TEAMMATES. If the tool consumer did not provide a return URL, TEAMMATES's LTI error page with
     * the error message is returned.
     *
     * <p>If the tool consumer did not provide a return URL, TEAMMATES's LTI error page is returned with
     * the error message specified.</p>
     *
     * @param ltiLaunchResult the launch result from the tool consumer
     */
    private ActionResult createNotInstructorErrorPageResult(LtiLaunch ltiLaunchResult) {
        final String launchPresentationReturnUrl = ltiLaunchResult.getLaunchPresentationReturnUrl();
        final String errorMessage = "Only instructors can access TEAMMATES";
        if (launchPresentationReturnUrl == null) {
            return createTeammatesLtiErrorPageResult(errorMessage);
        } else {
            String url = launchPresentationReturnUrl;
            url = Url.addParamToUrl(url, LTI_ERRORMSG, errorMessage);
            return createRedirectResult(url);
        }
    }

    /**
     * Returns a redirect back to the tool consumer with an error message and error log, indicating that the user cannot be
     * identified. If the tool consumer did not provide a return URL, TEAMMATES's LTI error page with the error message is
     * returned.
     *
     * <p>The error message indicates that the user cannot be identified and should be shown appropriately by the tool
     * consumer to the user. The error log indicates why the user cannot be identified and should not be shown to the user
     * but logged by the tool consumer.</p>
     *
     * <p>If the tool consumer did not provide a return URL, TEAMMATES's LTI error page is returned with
     * the error message specified.</p>
     *
     * @param ltiLaunchResult the launch result from the tool consumer
     */
    private ActionResult createCannotIdentifyUserErrorPageWithLogResult(LtiLaunch ltiLaunchResult) {
        final String errorMessage = "Unable to uniquely identify the user, please contact your administrator for help.";
        final String errorLog = "TEAMMATES require the user_id and roles parameter to uniquely identify the user.";

        log.severe(errorLog);

        final String launchPresentationReturnUrl = ltiLaunchResult.getLaunchPresentationReturnUrl();
        if (launchPresentationReturnUrl == null) {
            return createTeammatesLtiErrorPageResult(errorMessage);
        }

        String url = launchPresentationReturnUrl;
        url = Url.addParamToUrl(url, LTI_ERRORMSG, errorMessage);
        url = Url.addParamToUrl(url, LTI_ERRORLOG, errorLog);
        return createRedirectResult(url);
    }

    /**
     * Returns TEAMMATES's LTI error page with the specified errorMessage.
     * @param errorMessage - the error message to show in the page
     */
    private ActionResult createTeammatesLtiErrorPageResult(String errorMessage) {
        // TODO custom page data
        request.setAttribute("errorMessage", errorMessage);
        return createShowPageResult("/jsp/LtiErrorPage.jsp", new PageData(account, sessionToken));
    }

    private boolean isInstructorRole(String role) {
        return CONTEXT_ROLE_INSTRUCTOR_HANDLE.equals(role) || CONTEXT_ROLE_INSTRUCTOR_URN.equals(role)
                || CONTEXT_ROLE_LEARNER_INSTRUCTOR_HANDLE.equals(role) || CONTEXT_ROLE_LEARNER_INSTRUCTOR_URN.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_PRIMARY_INSTRUCTOR_HANDLE.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_PRIMARY_INSTRUCTOR_URN.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_LECTURER_HANDLE.equals(role) || CONTEXT_ROLE_INSTRUCTOR_LECTURER_URN.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_GUEST_INSTRUCTOR_HANDLE.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_GUEST_INSTRUCTOR_URN.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_EXTERNAL_INSTRUCTOR_HANDLE.equals(role)
                || CONTEXT_ROLE_INSTRUCTOR_EXTERNAL_INSTRUCTOR_URN.equals(role)
                || INSTITUTION_ROLE_INSTRUCTOR_URN.equals(role);
    }

    private ShowPageResult reconfirmInstructorDetails(HttpServletRequest req, LtiAccountAttributes ltiAccountAttributes) {
        InstructorsLogic instructorsLogic = InstructorsLogic.inst();
        final InstructorAttributes instructorAttributes = instructorsLogic
                .getInstructorForRegistrationKey(ltiAccountAttributes.getRegkey());

        AdminHomePageData data = new AdminHomePageData(null, null);
        data.instructorName = instructorAttributes.name;
        data.instructorEmail = instructorAttributes.email;
        String personInstitute = req.getParameter("tool_consumer_instance_name");
        if (personInstitute != null) {
            data.instructorInstitution = personInstitute;
        }
        data.setStatusMessagesToUser(
                Collections.singletonList(
                        new StatusMessage("You have a previous incomplete registration."
                                + " Please confirm your details again to proceed.", StatusMessageColor.INFO)));
        //req.setAttribute("data", data);
        req.setAttribute("regkey", ltiAccountAttributes.getRegkey());
        req.setAttribute("joinLink", Const.ActionURIs.INSTRUCTOR_COURSE_JOIN);

        return createShowPageResult("/jsp/LtiInstructorConfirmInstitute.jsp", data);
    }

    private ShowPageResult createInstructorAccount(HttpServletRequest req) {
        // TODO Add to Const.ViewURIs
        String retryUrl = "/jsp/LtiInstructorCreate.jsp";

        AdminHomePageData data = new AdminHomePageData(null, null);

        String personName = req.getParameter("lis_person_name_full");
        if (personName != null) {
            //retryUrl = Url.addParamToUrl(retryUrl, "name", personName);
            data.instructorName = personName;
        } else {
            personName = req.getParameter("lis_person_name_given");
            if (personName != null) {
                data.instructorName = personName;
                //retryUrl = Url.addParamToUrl(retryUrl, "name", personName);
            }
        }

        String personEmail = req.getParameter("lis_person_contact_email_primary");
        if (personEmail != null) {
            data.instructorEmail = personEmail;
            //retryUrl = Url.addParamToUrl(retryUrl, "email", personEmail);
        }

        String personInstitute = req.getParameter("tool_consumer_instance_name");
        if (personInstitute != null) {
            data.instructorInstitution = personInstitute;
            //retryUrl = Url.addParamToUrl(retryUrl, "institution", personInstitute);
        }

        //req.setAttribute("data", data);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", personName);
        parameters.put("email", personEmail);
        parameters.put("institution", personInstitute);

        String key = req.getParameter("oauth_consumer_key");

        final LtiOAuthCredentialDb ltiOAuthCredentialDb = new LtiOAuthCredentialDb();
        final LtiOAuthCredentialAttributes credential = ltiOAuthCredentialDb.getCredential(key);
        if (credential == null) {
            throw new RuntimeException("Credential should exist but does not exist");
            // this is not supposed to happen
        }

        String secret = credential.getConsumerSecret();

        LtiSigner ltiSigner = new LtiOauthSigner();
        try {
            Map<String, String> signedParameters = ltiSigner.signParameters(parameters, key, secret,
                    Config.getAppUrl(Const.ActionURIs.LTI_INSTRUCTOR_ACCOUNT_ADD).toAbsoluteString(),
                    "POST");
            // the parameters will be placed in the form itself
            signedParameters.remove("name");
            signedParameters.remove("email");
            signedParameters.remove("institution");
            req.setAttribute("signedParameters", signedParameters);
        } catch (LtiSigningException e) {
            throw new RuntimeException("Fail to sign!");
        }

        // TODO add token to post request

        return createShowPageResult(retryUrl, data);
    }

    private LtiVerificationResult verifyOAuth(HttpServletRequest req)
            throws LtiRequiredCredentialsNotFoundException, LtiVerificationException {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String oAuthConsumerKey = req.getParameter("oauth_consumer_key");

        if (oAuthConsumerKey == null) {
            throw new LtiRequiredCredentialsNotFoundException("Required post parameter oauth_consumer_key not found");
        }

        final LtiOAuthCredentialDb ltiOAuthCredentialDb = new LtiOAuthCredentialDb();
        final LtiOAuthCredentialAttributes credential = ltiOAuthCredentialDb.getCredential(oAuthConsumerKey);
        if (credential == null) {
            throw new LtiRequiredCredentialsNotFoundException("No consumer secret for the parameter oauth_consumer_key is "
                    + "found in the datastore");
        }

        String secret = credential.getConsumerSecret();

        LtiVerificationResult ltiResult = ltiVerifier.verify(req, secret);
        log.info("LTI status: " + ltiResult.getSuccess());
        log.info("Error: " + ltiResult.getError() + " Message: " + ltiResult.getMessage());
        return ltiResult;
    }

    @SuppressWarnings("unchecked")
    private void logParameters() {
        log.info("Printing parameters");

        for (Map.Entry<String, String[]> entry : requestParameters.entrySet()) {
            log.info("Parameter name: " + entry.getKey());
            for (String value : entry.getValue()) {
                log.info("Parameter value: " + value);
            }
        }
    }
}
