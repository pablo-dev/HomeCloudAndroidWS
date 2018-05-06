package planetludus.com.homecloudandroid;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

/**
 * Util class to manage the background job
 *
 * @author Pablo Carnero
 */
public class JobUtil {

    private final static String TAG = "JobUtil";
    private static int jobId = 0;

    /**
     * Schedule the job to be run every 10 - 30 seconds
     *
     * @param context
     */
    public static void scheduleJob(Context context) {
        Log.d(TAG, "scheduleJob");
        if (jobId != 0) {
            // job already running, do nothing
            Log.d(TAG, "scheduleJob: job already running");
            return;
        }

        ComponentName serviceComponent = new ComponentName(context, SyncService.class);
        JobInfo.Builder builder = new JobInfo.Builder(1, serviceComponent).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        builder.setMinimumLatency(1000);
        builder.setOverrideDeadline(5000);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobId = jobScheduler.schedule(builder.build());

        Log.d(TAG, "scheduleJob: job started with id: " + jobId);
    }

    /**
     * Stop the job
     *
     * @param context
     */
    public static void stopJob(Context context) {
        Log.d(TAG, "stopJob");
        if (jobId == 0) {
            // job already stopped, do nothing
            Log.d(TAG, "stopJob: job already stopped");
            return;
        }

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(jobId);
        Log.d(TAG, "stopJob: job with id " + jobId + " has been stopped");
        jobId = 0;
    }

    /**
     * Check if the job is running
     *
     * @param context
     * @return true if the job is running, otherwise false
     */
    public static boolean isRunning(Context context) {
        return ! (jobId == 0);
    }

}
