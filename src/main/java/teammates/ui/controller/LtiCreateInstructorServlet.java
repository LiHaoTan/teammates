package teammates.ui.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.imsglobal.lti.launch.LtiLaunch;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.imsglobal.pox.IMSPOXRequest;

import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Logger;
import teammates.logic.api.Logic;
import teammates.lti.LtiHelper;

import oauth.signpost.exception.OAuthException;

/**
 * Servlet to handle checking lti requests.
 */
@SuppressWarnings("serial")
public class LtiCreateInstructorServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger();

    private static final String LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";
    private static final String LTI_MSG = "lti_msg";
    private static final String LTI_LOG = "lti_log";
    private static final String LTI_ERRORMSG = "lti_errormsg";
    private static final String LTI_ERRORLOG = "lti_errorlog";

    private static final String INSTITUTION_ROLE_ADMIN_URN = "urn:lti:instrole:ims/lis/administrator";
    private static final String INSTITUTION_ROLE_ADMIN_HANDLE = "administrator";

    private static final String INSTITUTION_ROLE_INSTRUCTOR_URN = "urn:lti:instrole:ims/lis/Instructor";
    private static final String INSTITUTION_ROLE_INSTRUCTOR_HANDLE = "instructor";

    // Technically won't need to create this if this were an action, maybe we should have action
    private Logic logic = new Logic();

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req, resp);
    }

    @Override
    public final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // since we handle in another servlet we should do some checks again, which means we probably might not want to
        // be in another servlet actually as there is no point creating another request

        try {
            req.getRequestDispatcher("/jsp/LtiInstructorCreate.jsp").forward(req, resp);
        } catch (ServletException e) {
            resp.getWriter().println("Instructor Create redirect fail");
            return;
        }

/*        String instructorName = req.getParameter("lis_person_name_full");
        // note that you can contact your administrator for your details to be automatically filled

        // do a check if all the parameters are there, if it is not we make the user fill in a form
        // in any case we need the user to confirm before proceeding to do check and if it succeeds
        // then create the account with demo data, and ask them to sign in
        // with google account
        // alternatively, we can just use the user-id field from LTI since it is OAuth signed and not require the google
        // account at all

        try {
            logic.verifyInputForAdminHomePage(instructorName, instructorName, "", "noNeedEmail@email.com");
        } catch (InvalidParametersException e) {
            // print out e.message or something
            //e.printStackTrace();
        }


        //Is this an LTI launch request?
        //Is this a valid LTI launch request?
        //Are all the required parameters present?
        //Establish a user session
        //Redirect the user
        PrintWriter out = resp.getWriter();

        logParameters(req, out);

        LtiVerificationResult verificationResult = verifyOAuth(req, out);
        if (verificationResult == null || !verificationResult.getSuccess()) {
            log.warning("Invalid request LTI OAuth");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        final LtiLaunch ltiLaunchResult = verificationResult.getLtiLaunchResult();
        if (ltiLaunchResult.getUser().getId() == null || ltiLaunchResult.getUser().getRoles().size() == 0) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        log.info("All roles: " + req.getParameter("roles"));
        log.info("Unique user id: " + ltiLaunchResult.getUser().getId());
        for (String role : ltiLaunchResult.getUser().getRoles()) {
            log.info("User role: " + role);
            // to double check the spec if it is case sensitive, but it probably isn't
            if (INSTITUTION_ROLE_ADMIN_HANDLE.equalsIgnoreCase(role)
                    || INSTITUTION_ROLE_ADMIN_URN.equalsIgnoreCase(role)) {
                log.info("ADMIN FOUND! we might want to give them the option of doing something admin like or let"
                        + "them create instructor account");
            }
            if (INSTITUTION_ROLE_INSTRUCTOR_HANDLE.equalsIgnoreCase(role)
                    || INSTITUTION_ROLE_INSTRUCTOR_URN.equalsIgnoreCase(role)) {
                log.info("INSTRUCTOR FOUND!");
            }
        }*/

        //handleOutcomes(req, out);

        //handleLaunchPresentationReturn(req, resp, out);
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

    private LtiVerificationResult verifyOAuth(HttpServletRequest req, PrintWriter out) {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String key = req.getParameter("oauth_consumer_key");
        String secret = LtiHelper.secret; // get secret from key

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
    private void logParameters(HttpServletRequest req, PrintWriter out) {
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
