package planetludus.com.homecloudandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Activity to manage the activity settings
 *
 * @author Pablo Carnero
 */
public class Preferences extends Activity {

    private final static int MY_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedStateInstanceState) {
        super.onCreate(savedStateInstanceState);

        // display fragment as main content
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        // check permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
            ActivityCompat.requestPermissions(
                    this
                    , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                    , MY_READ_EXTERNAL_STORAGE);
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // load preference screen from xml directory.
            addPreferencesFromResource(R.xml.pref_general);

            // set action for switch button
            SwitchPreference syncPreference = (SwitchPreference) findPreference(getString(R.string.pref_key_sync));
            syncPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if ((Boolean) o) {
                        JobUtil.scheduleJob(getContext());
                    } else {
                        JobUtil.stopJob(getContext());
                    }
                    return true;
                }
            });

            // set action for about button
            Preference button = findPreference(getString(R.string.pref_key_about));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intentAbout = new Intent(getContext(), AboutActivity.class);
                    startActivity(intentAbout);
                    return true;
                }
            });

        }
    }
}
