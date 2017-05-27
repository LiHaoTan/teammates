package teammates.ui.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;

/**
 * Servlet to handle checking Canvas OAuth.
 */
@SuppressWarnings("serial")
public class CanvasServlet extends HttpServlet {


    /**
     * Canvas CLIENT_ID.
     */
    public static final String CLIENT_ID = "3516";
    /**
     * Canvas CLIENT_SECRET.
     */
    public static final String CLIENT_SECRET = "412ee35ae2a5469d3d21";
    /**
     * REDIRECT_URI.
     */
    public static final String REDIRECT_URI = "canvas-authorization";
    /**
     * ROOT_URL.
     */
    public static final String ROOT_URL = "http://127.0.0.1:8888";

    //private static final String ROOT_URL = "https://teammates-lihaotan.appspot.com";

    /**
     * PARAM_CLIENT_ID.
     */
    public static final String PARAM_CLIENT_ID = "client_id";
    /**
     * PARAM_CLIENT_SECRET.
     */
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    /**
     * PARAM_RESPONSE_TYPE.
     */
    public static final String PARAM_RESPONSE_TYPE = "response_type";
    /**
     * PARAM_REDIRECT_URI.
     */
    public static final String PARAM_REDIRECT_URI = "redirect_uri";

    private static final String RESPONSE_TYPE = "code";

    private static final String OAUTH_INIT_URL = "https://learn-lti.herokuapp.com/login/oauth2/auth";

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try {
            URIBuilder uriBuilder = new URIBuilder(OAUTH_INIT_URL);
            uriBuilder.addParameter(PARAM_CLIENT_ID, CLIENT_ID);
            uriBuilder.addParameter(PARAM_RESPONSE_TYPE, RESPONSE_TYPE);
            uriBuilder.addParameter(PARAM_REDIRECT_URI, ROOT_URL + "/" + REDIRECT_URI);
            resp.sendRedirect(uriBuilder.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
