package planetludus.com.homecloudandroid;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;

public class HttpUtils {

    private static final String LOGIN_SERVICE = "Login";
    private static final String LAST_UPDATE_SERVICE = "LastUpdate";
    private static final String POST_IMAGE_SERVICE = "New";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String URL_PATH = "SyncService";
    private static final String EMPTY_STRING = "";
    private static final int BASE_64_FLAGS = Base64.DEFAULT;

    private String token;
    private String baseUrl;
    private int bufferSize;

    public HttpUtils(String serverName, String port, int bufferSize) {
        this.bufferSize = bufferSize;
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

    private void setInput(HttpURLConnection conn, String jsonInput) throws IOException {
        OutputStream os = conn.getOutputStream();
        os.write(jsonInput.getBytes());
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

    private String post(String serviceName, String jsonInput, String getParam)
            throws IOException, JSONException, AuthenticationException {
        HttpURLConnection conn = null;
        try {
            conn = getConnection(serviceName, jsonInput.getBytes().length);
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
        StringBuffer jsonInput = new StringBuffer()
                .append("{")
                .append("\"userName\":")
                .append(JSONObject.quote(userName))
                .append(",")
                .append("\"password\":")
                .append(JSONObject.quote(password))
                .append("}");

        this.token = post(LOGIN_SERVICE, jsonInput.toString(), "LoginResult");
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
        StringBuffer jsonInput = new StringBuffer()
                .append("{")
                .append("\"token\":")
                .append(JSONObject.quote(this.token))
                .append("}");

        return post(LAST_UPDATE_SERVICE, jsonInput.toString(), "GetLastSyncResult");
    }

    /**
     * Post the base64 image with the given fileName
     * This buffer size should be divisible by three see (https://stackoverflow.com/a/39099064/3790546)
     *
     * @param file
     * @param fileName
     * @param lastModified
     * @throws JSONException
     *          Error parsing the json output
     * @throws IOException
     *          Connectivity error
     * @throws AuthenticationException
     *          Invalid user pass
     */
    public void postImage(File file, String fileName, String lastModified)
            throws  JSONException, IOException, AuthenticationException {

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), this.bufferSize)) {
            byte[] chunk = new byte[this.bufferSize];
            int len;
            String idPart = file.length() > this.bufferSize ? generatePartId(20) : EMPTY_STRING;
            while ((len = in.read(chunk)) == this.bufferSize) {
                String jsonInput =
                        postImageJson(Base64.encodeToString(chunk, BASE_64_FLAGS), fileName, lastModified, idPart);
                post(POST_IMAGE_SERVICE, jsonInput, EMPTY_STRING);
            }
            if (len > 0) {
                String jsonInput =
                        postImageJson(Base64.encodeToString(Arrays.copyOf(chunk, len), BASE_64_FLAGS), fileName, lastModified, EMPTY_STRING);
                post(POST_IMAGE_SERVICE, jsonInput, EMPTY_STRING);

                if (! EMPTY_STRING.equals(idPart)) {
                    jsonInput =
                            postImageJson(EMPTY_STRING, fileName, lastModified, idPart);
                    post(POST_IMAGE_SERVICE, jsonInput, EMPTY_STRING);
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    private String generatePartId(int length) {
        String result = "";
        Random r = new Random();
        for (int i = 0; i < length; i++) {
            result += r.nextInt(10);
        }
        return result;
    }

    private String postImageJson(String chunk, String fileName, String lastModified, String idPart) {
        StringBuilder jsonInput = new StringBuilder()
                .append("{")
                .append("\"imageBase64\":")
                .append(JSONObject.quote(chunk))
                .append(",")
                .append("\"token\":")
                .append(JSONObject.quote(this.token))
                .append(",")
                .append("\"fileName\":")
                .append(JSONObject.quote(fileName))
                .append(",")
                .append("\"lastModified\":")
                .append(JSONObject.quote(lastModified))
                .append(",")
                .append("\"idPart\":")
                .append(JSONObject.quote(idPart))
                .append("}");

        return jsonInput.toString();
    }
}
