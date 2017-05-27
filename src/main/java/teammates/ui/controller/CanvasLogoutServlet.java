package teammates.ui.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;

/**
 * Servlet to handle checking Canvas OAuth.
 */
@SuppressWarnings("serial")
public class CanvasLogoutServlet extends HttpServlet {

    private static final String OAUTH_TOKEN_URL = "https://learn-lti.herokuapp.com/login/oauth2/token";

    private static final String ACCESS_TOKEN = "0cc4549e1794e59d4443_3516";
    private static final String ROOT_URL = "http://127.0.0.1:8888";

    private static final String PARAM_ACCESS_TOKEN = "access_token";

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        try {
            URIBuilder uriBuilder = new URIBuilder(OAUTH_TOKEN_URL);
            uriBuilder.addParameter(PARAM_ACCESS_TOKEN, ACCESS_TOKEN);

            HttpClient httpclient = HttpClients.createDefault();
            HttpDelete httpDelete = new HttpDelete(uriBuilder.build());
            // Bearer does not work on sample URLs, we try next time
            //HttpDelete httpDelete = new HttpDelete(OAUTH_TOKEN_URL);
            //httpDelete.addHeader("Authorization", "Bearer " + ACCESS_TOKEN);

            HttpResponse response = httpclient.execute(httpDelete);
            out.println("Status code: " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();

            //Execute and get the response.
            if (entity != null) {
                InputStream inputStream = entity.getContent();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
