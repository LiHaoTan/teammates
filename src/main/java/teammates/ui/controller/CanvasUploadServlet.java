package teammates.ui.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Servlet to handle checking Canvas OAuth.
 */
@SuppressWarnings("serial")
public class CanvasUploadServlet extends HttpServlet {

    private static final String PREFLIGHT_URL = "https://canvas.instructure.com/api/v1/folders/8919241/files";

    private static final String ACCESS_TOKEN = "7~NRJJ9aKkb9LYsA9WreM3ht32z0iBSYBzfQox5BPPSofqN2w6vmghZltONQQX0ICO";

    @Override
    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        uploadSemiWorkingFile(resp);
        //uploadSemiWorkingUrl(resp);
    }

    private void uploadSemiWorkingFile(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(PREFLIGHT_URL + "?access_token=" + ACCESS_TOKEN);

        byte[] slitheringFile = new byte[] {0x48, 0x45, 0x4c, 0x4c, 0x4f};

        byte[] slitherRepeat = new byte[200];

        for (int i = 0; i < slitherRepeat.length / slitheringFile.length; i++) {
            for (int j = 0; j < slitheringFile.length; j++) {
                slitherRepeat[i * slitheringFile.length + j] = slitheringFile[j];
            }
        }

        String fileName = "call_him_dr_jones.doll";
        String contentType = "application/short-round";

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(new BasicNameValuePair("name", fileName));
        params.add(new BasicNameValuePair("size", "" + slitherRepeat.length));
        params.add(new BasicNameValuePair("content_type", contentType));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        //Execute and get the response.
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity jsonEntity = response.getEntity();

        if (jsonEntity != null) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder
                    .create();

            String jsonString = entityToString(jsonEntity);

            if (jsonString == null) {
                out.println("We can't handle this null!!");
                return;
            }

            JsonParser parser = new JsonParser();
            JsonObject rootObj = parser.parse(jsonString).getAsJsonObject();

            JsonPrimitive urlObj = rootObj.getAsJsonPrimitive("upload_url");
            JsonObject paramsObj = rootObj.getAsJsonObject("upload_params");

            for (Map.Entry<String, JsonElement> entry : paramsObj.entrySet()) {
                //out.println(entry.getKey() + "/" + entry.getValue().getAsString());
                entityBuilder.addTextBody(entry.getKey(), entry.getValue().getAsString());
            }

            entityBuilder.addBinaryBody("file", slitherRepeat,
                    ContentType.create(contentType), fileName);
            HttpEntity entity = entityBuilder.build();

            // Upload step
            //out.println(urlObj.getAsString());
            HttpPost httpPostUpload = new HttpPost(urlObj.getAsString());
            httpPostUpload.setEntity(entity);
            HttpClient httpClientUpload = HttpClients.createDefault();
            HttpResponse uploadResponse = httpClientUpload.execute(httpPostUpload);

            out.println("Status code: " + uploadResponse.getStatusLine());
            HttpEntity entityTest = uploadResponse.getEntity();

            if (entityTest != null) {
                out.println("Upload Complete!");
                out.println(entityToString(entityTest));
            }

            /*out.println("Headers");
            Header[] headers = uploadResponse.getAllHeaders();

            for (Header header : headers) {
                out.println(header);
            }*/
            //HttpEntity responseEntity = response.getEntity();

        } else {
            out.println("Something wrong");
        }
    }

    /**
     * Note that these is not correctly.
     */
    private void uploadSemiWorkingUrl(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(PREFLIGHT_URL + "?access_token=9f683adc137279686966_3516");

        byte[] slitheringFile = new byte[] {0x48, 0x45, 0x4c, 0x4c, 0x4f};

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(4);
        params.add(new BasicNameValuePair("name", "monkey.brains"));
        params.add(new BasicNameValuePair("size", "" + 12345));
        params.add(new BasicNameValuePair("content_type", "application/chilled-dessert"));
        params.add(new BasicNameValuePair("url", "http://www.example.com/files/monkey.brains"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity jsonEntity = response.getEntity();

        if (jsonEntity != null) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder
                    .create();

            String jsonString = entityToString(jsonEntity);
            out.println(jsonString);

            JsonParser parser = new JsonParser();
            JsonObject rootObj = parser.parse(jsonString).getAsJsonObject();

            JsonPrimitive urlObj = rootObj.getAsJsonPrimitive("status_url");

            try {
                URIBuilder uriBuilder = new URIBuilder(urlObj.getAsString());
                uriBuilder.addParameter("access_token", ACCESS_TOKEN);
                HttpGet httpGet = new HttpGet(uriBuilder.build());
                HttpResponse httpResponse = httpclient.execute(httpGet);

                HttpEntity entity = httpResponse.getEntity();

                if (entity != null) {
                    out.println(entityToString(entity));
                }

            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        } else {
            out.println("Something wrong");
        }
    }

    private String entityToString(HttpEntity entity) {
        try {
            InputStream inputStream = entity.getContent();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            String resultString = result.toString("UTF-8");
            inputStream.close();

            return resultString;
        } catch (IOException e) {
            e.printStackTrace(); // we can't handle this

            return null;
        }
    }
}
