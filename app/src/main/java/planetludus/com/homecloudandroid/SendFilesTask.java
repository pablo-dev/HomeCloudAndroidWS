package planetludus.com.homecloudandroid;

import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Task to send the files over the network in background
 *
 * @author Pablo Carnero
 */

public class SendFilesTask extends AsyncTask<String, Integer, Boolean> {

    private final static String TAG = "SendFilesTask";
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Context context;

    public SendFilesTask(Context context) {
        super();
        this.context = context;
    }

    /**
     * Copy the files in background.
     *
     * @param params first param is ip
     *               second param is port
     *               third param is user id
     *               fourth param is password
     *               fifth param is the buffer size
     * @return a result, defined by the subclass of this task.
     */
    @Override
    protected Boolean doInBackground(String... params) {

        // input parameters
        if (params == null || params.length != 5) {
            Log.e(TAG, "doInBackground: Input param error: "
                    + params == null ? "Null" : String.valueOf(params.length));
            return false;
        }

        String ip = params[0];
        String port = params[1];
        String id = params[2];
        String password = params[3];
        int bufferSize = Integer.parseInt(params[4]);

        // initialize notifications
        NotificationManager mNotifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_info_black_24dp);

        try {
            // get token
            HttpUtils httpUtils = new HttpUtils(ip, port, bufferSize);
            httpUtils.getToken(id, password);

            // get last sync
            Date lastSyncDate = dateFormatter.parse(httpUtils.getLastSync());
            List<File> fileList = new ArrayList<>();
            fileList.addAll(getMediaFrom(lastSyncDate, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            fileList.addAll(getMediaFrom(lastSyncDate, MediaStore.Video.Media.EXTERNAL_CONTENT_URI));
            Collections.sort(fileList, new MediaItemComparator());

            // update progress bar
            if (fileList.size() > 0){
                mBuilder.setProgress(fileList.size(), 0, true);
            }

            Log.d(TAG, "doInBackground: We are going to send " + fileList.size() + " files.");

            // send all the files
            for (File file : fileList) {
                Log.d(TAG, "doInBackground: Sending the file: " + file.getName() + " size: " + file.length());

                Date lastModified = new Date(file.lastModified());

                try {
                    httpUtils.postImage(file, file.getName(), dateFormatter.format(lastModified));
                } catch (OutOfMemoryError ex) {
                    Log.e(TAG, ex.getClass().toString(), ex);
                    mBuilder.setContentText(context.getString(R.string.notification_error_file_too_big));
                    mNotifyManager.notify(1, mBuilder.build());
                }

                // update progress bar
                mBuilder.setProgress(fileList.size(), 1, true);
            }

            Log.d(TAG, "doInBackground: All the files sent");
            // removes the progress bar
            if (fileList.size() > 0) {
                mBuilder.setContentText(context.getString(R.string.notification_sync_completed))
                        .setProgress(0, 0, false);
                mNotifyManager.notify(1, mBuilder.build());
            }

            return true;

        } catch (MalformedURLException | JSONException | AuthenticationException
                | ParseException ex) {
            Log.e(TAG, ex.getClass().toString(), ex);
            mBuilder.setContentText(getMessageException(ex))
                    .setProgress(0, 0, false);
            mNotifyManager.notify(1, mBuilder.build());
            return false;
        } catch (IOException ex) {
            Log.e(TAG, ex.getClass().toString(), ex);
            mBuilder.setContentText(getMessageException(ex))
                    .setProgress(0, 0, false);
            mNotifyManager.notify(1, mBuilder.build());
            return false;
        } catch (Exception ex) {
            Log.e(TAG, ex.getClass().toString(), ex);
            mBuilder.setContentText(getMessageException(ex))
                    .setProgress(0, 0, false);
            mNotifyManager.notify(1, mBuilder.build());
            return false;
        }

    }

    /**
     * Get the appropriate message for each exception type
     * @param ex
     * @return
     */
    private String getMessageException(Exception ex) {
        if (ex instanceof MalformedURLException) {
            return context.getString(R.string.notification_error_malformed_url);
        } else if (ex instanceof JSONException) {
            return context.getString(R.string.notification_error_generic);
        } else if (ex instanceof AuthenticationException) {
            return context.getString(R.string.notification_error_incorrect_userpass);
        } else if (ex instanceof ParseException) {
            return context.getString(R.string.notification_error_date_format_error);
        } else if (ex instanceof IOException) {
            return context.getString(R.string.notification_error_service_error);
        } else {
            return context.getString(R.string.notification_error_undefined_error);
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    /**
     * Get the file list from folder newer than date
     *
     * @param date Min date of files to be get
     * @param uri media Uri, typically
     *            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
     *            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
     *            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
     * @return file list
     */
    private List<File> getMediaFrom(Date date, Uri uri) {

        String[] projection = { MediaStore.MediaColumns.DATA,
                                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                                MediaStore.Images.Media.DATE_MODIFIED};
        Cursor cursor = context.getContentResolver().query(uri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_MODIFIED);

        List<File> fileList = new ArrayList<>();
        int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            String absolutePathOfImage = cursor.getString(column_index_data);
            File currentImage = new File(absolutePathOfImage);
            Date dateFile = new Date(currentImage.lastModified());
            if (dateFile.compareTo(date) > 0) {
                fileList.add(currentImage);
            }
        }

        return fileList;
    }

    private class MediaItemComparator implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            if (lhs.lastModified() == rhs.lastModified()) {
                return 0;
            } else if (lhs.lastModified() > rhs.lastModified()) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
