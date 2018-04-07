package planetludus.com.homecloudandroid;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUtils {

    private static final String LOGIN_SERVICE = "Login";
    private static final String LAST_UPDATE_SERVICE = "LastUpdate";
    private static final String POST_IMAGE_SERVICE = "New";
    private static final String UPDATE_LAST_SYNC = "UpdateLastSync";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String URL_PATH = "SyncService";
    private static final String EMPTY_STRING = "";

    private String token;
    private String baseUrl;

    public HttpUtils(String serverName, String port) throws MalformedURLException {
        this.baseUrl = new StringBuilder(HTTP_PROTOCOL)
                .append(serverName)
                .append(":")
                .append(port)
                .append("/")
                .append(URL_PATH)
                .append("/")
                .toString();
    }

    private HttpURLConnection getConnection(String serviceName, long size) throws IOException {
        String url = new StringBuilder(baseUrl).append(serviceName).toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (size > 0) conn.setFixedLengthStreamingMode(size);
        return conn;
    }

    private void setInput(HttpURLConnection conn, JSONObject jsonInput) throws IOException {
        OutputStream os = conn.getOutputStream();
        os.write(jsonInput.toString().getBytes());
        os.flush();
        os.close();
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

    private String post(String serviceName, JSONObject jsonInput, String getParam)
            throws IOException, JSONException, AuthenticationException {
        HttpURLConnection conn = null;
        try {
            conn = getConnection(serviceName, jsonInput.toString().getBytes().length);
            setInput(conn, jsonInput);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (! EMPTY_STRING.equals(getParam)) {
                    return getOutput(conn).getString(getParam);
                } else {
                    return EMPTY_STRING;
                }
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

    /**
     * Login and get token to be used in the further calls
     *
     * @param userName
     * @param password
     * @throws JSONException
     *          Error parsing the json output
     * @throws IOException
     *          Connectivity error
     * @throws AuthenticationException
     *          Invalid user pass
     */
    public void getToken(String userName, String password) throws JSONException, IOException, AuthenticationException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("userName", userName);
        jsonInput.put("password", password);

        this.token = post(LOGIN_SERVICE, jsonInput, "LoginResult");
    }

    /**
     * Get the las synchronization date of the user logged with the given token
     *
     * @return The last synchronization date
     * @throws JSONException
     *          Error parsing the json output
     * @throws IOException
     *          Connectivity error
     * @throws AuthenticationException
     *          Invalid user pass
     */
    public String getLastSync() throws JSONException, IOException, AuthenticationException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("token", this.token);

        return post(LAST_UPDATE_SERVICE, jsonInput, "GetLastSyncResult");
    }

    /**
     * Post the base64 image with the given fileName
     *
     * @param imageBase64
     * @param fileName
     * @throws JSONException
     *          Error parsing the json output
     * @throws IOException
     *          Connectivity error
     * @throws AuthenticationException
     *          Invalid user pass
     */
    public void postImage(String imageBase64, String fileName, String lastModified)
            throws  JSONException, IOException, AuthenticationException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("imageBase64", imageBase64);
        jsonInput.put("token", this.token);
        jsonInput.put("fileName", fileName);
        jsonInput.put("lastModified", lastModified);

        post(POST_IMAGE_SERVICE, jsonInput, EMPTY_STRING);
    }

    public void updateLastSync() throws JSONException, IOException, AuthenticationException {
        JSONObject jsonInput = new JSONObject();
        jsonInput.put("token", this.token);

        post(UPDATE_LAST_SYNC, jsonInput, EMPTY_STRING);
    }

}
