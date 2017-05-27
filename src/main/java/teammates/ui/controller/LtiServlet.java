package teammates.ui.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.imsglobal.lti.launch.LtiVerificationResult;
import org.imsglobal.lti.launch.LtiVerifier;
import org.imsglobal.pox.IMSPOXRequest;

import teammates.lti.LtiHelper;

import oauth.signpost.exception.OAuthException;

/**
 * Servlet to handle checking lti requests.
 */
@SuppressWarnings("serial")
public class LtiServlet extends HttpServlet {

    private static final String LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";
    private static final String LTI_MSG = "lti_msg";
    private static final String LTI_LOG = "lti_log";
    private static final String LTI_ERRORMSG = "lti_errormsg";
    private static final String LTI_ERRORLOG = "lti_errorlog";

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req, resp);
    }

    @Override
    public final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //Is this an LTI launch request?
        //Is this a valid LTI launch request?
        //Are all the required parameters present?
        //Establish a user session
        //Redirect the user
        PrintWriter out = resp.getWriter();

        if (!verifyOAuth(req, out)) {
            out.println("Not valid we should do something");
        }

        printParameters(req, out);

        out.println(req.getParameter("oauth_consumer_key"));

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

        //final String launchPresentationReturnUrlParam = req.getParameter(LAUNCH_PRESENTATION_RETURN_URL);
        final String launchPresentationReturnUrlParam = null;
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

    private boolean verifyOAuth(HttpServletRequest req, PrintWriter out) {
        LtiVerifier ltiVerifier = new LtiOauthVerifier();
        String key = req.getParameter("oauth_consumer_key");
        String secret = LtiHelper.secret; // get secret from key


        LtiVerificationResult ltiResult;
        try {
            ltiResult = ltiVerifier.verify(req, secret);
            out.println("Success: " + ltiResult.getSuccess());
            out.println("Error: " + ltiResult.getError());
            out.println("Message: " + ltiResult.getMessage());
            //out.println("Launch result: " + ltiResult.getLtiLaunchResult().);
            return ltiResult.getSuccess();
        } catch (LtiVerificationException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void printParameters(HttpServletRequest req, PrintWriter out) {
        out.println("Printing Parameters<br>");

        Map<String, String[]> parameterMap = req.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            out.println("Parameter name: " + entry.getKey() + "<br>");
            for (String value : entry.getValue()) {
                out.println("Parameter value: " + value + "<br>");
            }
        }
    }
}
