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
import java.net.ProtocolException;
import java.net.URL;

public class HttpUtils {

    private final String LOGIN_SERVICE = "Login";
    private final String POST_IMAGE_SERVICE = "New";
    private final String LAST_UPDATE_SERVICE = "LastUpdate";

    private String token;
    private String baseUrl;

    public HttpUtils(String serverName, String port) throws MalformedURLException {
        this.baseUrl = new StringBuilder("http://")
                .append(serverName)
                .append(":")
                .append(port)
                .append("/SyncService/")
                .toString();
    }

    private HttpURLConnection getConnection() throws IOException {
        String url = new StringBuilder(baseUrl).append(LOGIN_SERVICE).toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }

    private void setInput(HttpURLConnection conn, JSONObject jsonInput) throws IOException {
        OutputStream os = conn.getOutputStream();
        os.write(jsonInput.toString().getBytes());
        os.flush();
    }

    private JSONObject getOutput(HttpURLConnection conn) throws IOException, JSONException {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        return new JSONObject(sb.toString());
    }

    public void getToken(String userName, String password) throws JSONException, IOException, AuthenticationException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("userName", userName);
        jsonInput.put("password", password);

        HttpURLConnection conn = null;
        try {
            conn = getConnection();
            setInput(conn, jsonInput);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                token = getOutput(conn).getString("LoginResult");
            } else if (conn.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new AuthenticationException("Incorrect user/pass");
            } else {
                throw new IOException("The service return "
                        + conn.getResponseCode() + "\n"
                        + conn.getResponseMessage());
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // TODO: implement other methods
}
