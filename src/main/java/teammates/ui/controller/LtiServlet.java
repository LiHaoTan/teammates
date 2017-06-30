package teammates.ui.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.imsglobal.lti.launch.LtiLaunch;
import org.imsglobal.lti.launch.LtiOauthSigner;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiSigner;
import org.imsglobal.lti.launch.LtiSigningException;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.imsglobal.pox.IMSPOXRequest;

import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.LtiAccountAttributes;
import teammates.common.datatransfer.attributes.LtiOAuthCredentialAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.Logger;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.common.util.StringHelper;
import teammates.common.util.Url;
import teammates.logic.core.InstructorsLogic;
import teammates.lti.LtiHelper;
import teammates.storage.api.LtiAccountDb;
import teammates.storage.api.LtiOAuthCredentialDb;
import teammates.ui.pagedata.AdminHomePageData;

import oauth.signpost.exception.OAuthException;

/**
 * Servlet to handle checking lti requests.
 */
@SuppressWarnings("serial")
public class LtiServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger();

    private static final String LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";
    @SuppressWarnings("unused")
    private static final String LTI_MSG = "lti_msg";
    private static final String LTI_LOG = "lti_log";
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
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req, resp);
    }

    @Override
    public final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logParameters(req);

        LtiVerificationResult verificationResult = verifyOAuth(req);
        if (verificationResult == null || !verificationResult.getSuccess()) {
            log.warning("Invalid request LTI OAuth");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        final LtiLaunch ltiLaunchResult = verificationResult.getLtiLaunchResult();
        if (ltiLaunchResult.getUser().getId() == null || ltiLaunchResult.getUser().getRoles().size() == 0) {
            final String launchPresentationReturnUrl = ltiLaunchResult.getLaunchPresentationReturnUrl();
            if (launchPresentationReturnUrl == null) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String url = launchPresentationReturnUrl;
                url = Url.addParamToUrl(url, LTI_ERRORMSG, "Unable to uniquely identify the user, please contact your"
                        + " administrator for help.");
                url = Url.addParamToUrl(url, LTI_ERRORLOG, "TEAMMATES require the user_id and roles parameter to"
                        + "uniquely identify the user.");
                resp.sendRedirect(url);
            }
            return;
        }

        log.info("All roles: " + req.getParameter("roles"));
        log.info("Unique user id: " + ltiLaunchResult.getUser().getId());

        req.getSession().setAttribute("access_type", "lti");

        for (String role : ltiLaunchResult.getUser().getRoles()) {
            log.info("User role: " + role);
            if (isInstructorRole(role)) {
                log.info("INSTRUCTOR FOUND!");
                String userId = ltiLaunchResult.getUser().getId();

                req.getSession().setAttribute("role", "instructor");
                req.getSession().setAttribute("user_id", userId);

                LtiAccountDb ltiAccountDb = new LtiAccountDb();
                LtiAccountAttributes ltiAccountAttributes = ltiAccountDb.getAccount(userId);
                if (ltiAccountAttributes == null) {
                    try {
                        ltiAccountDb.createAccount(new LtiAccountAttributes(userId));
                    } catch (InvalidParametersException e) {
                        log.severe("Can't create LtiAccount");
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                    createInstructorAccount(req, resp);
                } else if (ltiAccountAttributes.getGoogleId() != null) {
                    resp.sendRedirect(Const.ActionURIs.INSTRUCTOR_HOME_PAGE);
                } else if (ltiAccountAttributes.getRegkey() != null) {
                    reconfirmInstructorDetails(req, resp, userId, ltiAccountAttributes);
                } else {
                    createInstructorAccount(req, resp);
                }

                break;
            }
        }

        //handleOutcomes(req, out);

        //handleLaunchPresentationReturn(req, resp, out);
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

    private void reconfirmInstructorDetails(HttpServletRequest req, HttpServletResponse resp, String userId,
                                            LtiAccountAttributes ltiAccountAttributes) throws IOException {
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
        req.setAttribute("data", data);
        req.setAttribute("regkey", ltiAccountAttributes.getRegkey());
        req.setAttribute("joinLink", Const.ActionURIs.INSTRUCTOR_COURSE_JOIN);

        try {
            req.getRequestDispatcher("jsp/LtiInstructorConfirmInstitute.jsp").forward(req, resp);
        } catch (ServletException e) {
            log.severe(e.getMessage());
            log.severe("Lti confirm instructor fail");
        }
    }

    private void createInstructorAccount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // TODO Add to Const.ViewURIs
        String retryUrl = "jsp/LtiInstructorCreate.jsp";

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

        req.setAttribute("data", data);

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

        // add token to post request

        try {
            req.getRequestDispatcher(retryUrl).forward(req, resp);
        } catch (ServletException e) {
            log.severe("Lti create instructor fail");
        }
    }

    private String createJoinLinkFromInstructorRegKey(String instructorRegKey, String instructorInstitution) {
        return Config.getAppUrl(Const.ActionURIs.INSTRUCTOR_COURSE_JOIN)
                .withRegistrationKey(StringHelper.encrypt(instructorRegKey))
                .withInstructorInstitution(instructorInstitution)
                .toAbsoluteString();
    }

    private void handleLaunchPresentationReturn(HttpServletRequest req, HttpServletResponse resp, PrintWriter out)
            throws IOException {
        final String launchPresentationReturnUrlParam = req.getParameter(LAUNCH_PRESENTATION_RETURN_URL);
        if (launchPresentationReturnUrlParam != null) {
            try {
                // maybe we should include uriBuilder apache directly
                // maybe we should use URIUtils.createURI()
                // https://stackoverflow.com/questions/883136/what-is-the-idiomatic-way-to-compose-a-url-or-uri-in-java
                URIBuilder uriBuilder = new URIBuilder(launchPresentationReturnUrlParam);
                uriBuilder.addParameter(LTI_LOG, "One ping only.");
                //uriBuilder.addParameter(LTI_ERRORMSG, "Who's going to save you, Junior?!");
                //uriBuilder.addParameter(LTI_ERRORLOG, "The floor's on fire... see... *&* the chair.");

                uriBuilder.addParameter("embed_type", "oembed");
                uriBuilder.addParameter("url", "http://www.flickr.com/photos/bees/2341623661/");
                uriBuilder.addParameter("endpoint", "http://www.flickr.com/services/oembed/");
                resp.sendRedirect(uriBuilder.toString());
            } catch (URISyntaxException e) {
                out.println("Something wrong with the url");
                e.printStackTrace();
            }
        }
    }

    private void handleOutcomes(HttpServletRequest req, PrintWriter out) throws IOException {
        if (req.getParameter("lis_outcome_service_url") != null && req.getParameter("lis_result_sourcedid") != null) {
            //send Request directly
            try {
                IMSPOXRequest.sendReplaceResult(req.getParameter("lis_outcome_service_url"),
                        req.getParameter("oauth_consumer_key"), LtiHelper.secret,
                        req.getParameter("lis_result_sourcedid"), "0.43", "http://www.example.com/horcruxes/8", true);
            } catch (OAuthException | GeneralSecurityException e) {
                out.println("Something wrong sending back grades");
                e.printStackTrace();
            }
        }
    }

    private LtiVerificationResult verifyOAuth(HttpServletRequest req) {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String key = req.getParameter("oauth_consumer_key");

        if (key == null) {
            return null;
        }

        final LtiOAuthCredentialDb ltiOAuthCredentialDb = new LtiOAuthCredentialDb();
        final LtiOAuthCredentialAttributes credential = ltiOAuthCredentialDb.getCredential(key);
        if (credential == null) {
            return null;
        }

        String secret = credential.getConsumerSecret();

        LtiVerificationResult ltiResult;
        try {
            ltiResult = ltiVerifier.verify(req, secret);
            log.info("LTI status: " + ltiResult.getSuccess());
            log.info("Error: " + ltiResult.getError() + " Message: " + ltiResult.getMessage());
            return ltiResult;
        } catch (LtiVerificationException e) {
            log.warning("Something went wrong verifying, message: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void logParameters(HttpServletRequest req) {
        log.info("Printing parameters");

        Map<String, String[]> parameterMap = req.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            log.info("Parameter name: " + entry.getKey());
            for (String value : entry.getValue()) {
                log.info("Parameter value: " + value);
            }
        }
    }
}
