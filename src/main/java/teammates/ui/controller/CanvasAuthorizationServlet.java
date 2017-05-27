package teammates.ui.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.base.Charsets;

/**
 * Servlet to handle checking Canvas OAuth.
 */
@SuppressWarnings("serial")
public class CanvasAuthorizationServlet extends HttpServlet {

    private static final String OAUTH_INIT_URL = "https://learn-lti.herokuapp.com/login/oauth2/auth";
    private static final String OAUTH_TOKEN_URL = "https://learn-lti.herokuapp.com/login/oauth2/token";

    private static final String CLIENT_ID = "3516";
    private static final String RESPONSE_TYPE = "code";
    private static final String REDIRECT_URI = "canvas-fail";
    //private static final String ROOT_URL = "https://teammates-lihaotan.appspot.com";
    private static final String ROOT_URL = "http://127.0.0.1:8888";

    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_RESPONSE_TYPE = "response_type";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        if ("access_denied".equals(req.getParameter("error"))) {
            resp.sendRedirect("/canvas-fail");
        } else {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(OAUTH_TOKEN_URL);

            // Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(3);
            params.add(new BasicNameValuePair(CanvasServlet.PARAM_CLIENT_ID, CanvasServlet.CLIENT_ID));
            params.add(new BasicNameValuePair(CanvasServlet.PARAM_CLIENT_SECRET, CanvasServlet.CLIENT_SECRET));
            params.add(new BasicNameValuePair(CanvasServlet.PARAM_REDIRECT_URI,
                    CanvasServlet.ROOT_URL + "/" + CanvasServlet.REDIRECT_URI));
            params.add(new BasicNameValuePair("code", req.getParameter("code")));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream inputStream = entity.getContent();

                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                try {
                    out.println(result.toString("UTF-8"));
                    // do something useful
                } finally {
                    inputStream.close();
                }
            }
            //resp.sendRedirect("/canvas-success"); // not needed


        }
    }
}
