package planetludus.com.homecloudandroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUtils {

    private URL url;
    private String token;

    public HttpUtils(String serverName, String port) throws MalformedURLException {
        String urlConnection = new StringBuilder("http://")
                .append(serverName)
                .append(":")
                .append(port)
                .append("/")
                .toString();
        url = new URL(urlConnection);
    }

    public void getToken(String userName, String password) throws JSONException, IOException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("userName", userName);
        jsonInput.put("password", password);

        HttpURLConnection conn = null;
        OutputStream os = null;
        InputStream is = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout( 10000 /*milliseconds*/ );
            conn.setConnectTimeout( 15000 /* milliseconds */ );
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(jsonInput.toString().getBytes().length);

            // make some HTTP header nicety
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            // open
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                JSONObject result = new JSONObject(sb.toString());
                token = result.getString("LoginResult");
            } else {
                throw new IOException("The service return "
                        + conn.getResponseCode() + "\n"
                        + conn.getResponseMessage());
            }

        } finally {
            //clean up
            if (os != null) os.close();
            if (is != null) is.close();
            if (conn != null) conn.disconnect();
        }
    }

}
