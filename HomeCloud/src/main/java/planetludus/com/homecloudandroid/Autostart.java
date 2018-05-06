package planetludus.com.homecloudandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class used to receive the boot action and schedule the job
 *
 * @author Pablo Carnero
 */
public class Autostart extends BroadcastReceiver {

    private static final String TAG = "Autostart";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Broadcast received");

        // check sync preference status
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean sync = prefs.getBoolean(context.getString(R.string.pref_key_sync), false);
        
        // start the service
        if (sync) {
            Log.d(TAG, "onReceive: Starting the sync service");
            JobUtil.scheduleJob(context);
        }
    }
}
