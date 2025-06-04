package io.github.charmicat.vaghelper;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.List;

@SuppressWarnings("unused")
public class HelperFunctions {

    private static final String TAG = "HelperFunctions";

    // Determine if servicename is currently running
    public static boolean isServiceRunning(Context context, String servicename, boolean debug) {
        if (debug) {
            Log.d(TAG, "isServiceRunning context: " + context + " servicename: " + servicename);
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (debug)
                Log.d(TAG, service.service.getClassName());
            if (servicename.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context context, String servicename) {
        return isServiceRunning(context, servicename, false);
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action  Name of the Intent action to be checked for availability.
     * @param debug Enable/disable debug messages.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action, boolean debug) {
        if (debug) {
            Log.d(TAG, "isIntentAvailable context: " + context + " action: " + action);
        }
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static boolean isIntentAvailable(Context context, String action) {
        return isIntentAvailable(context, action, false);
    }

    private static Intent rateIntentForUrl(Context c, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, c.getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        intent.addFlags(flags);
        return intent;
    }

    /**
     * Creates a Google Play Store rating intent for the specified app context
     * @param context The application's environment.
     * @param debug Enable/disable debug messages.
     * @return the rating intent
     */
    public static Intent rateApp(Context context, boolean debug) {
        if (debug) {
            Log.d(TAG, "rateApp context: " + context);
        }
        Intent rateIntent;
        // google play
        try {
            rateIntent = rateIntentForUrl(context, "market://details");
            rateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            rateIntent = rateIntentForUrl(context, "http://play.google.com/store/apps/details");
            rateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(rateIntent);
        }

        return rateIntent;
    }
    public static Intent rateApp(Context context) {
        return rateApp(context,false);
    }

    /**
     * Plays an audio file
     * @param context The application's environment.
     * @param resourceID The raw resource id (R.raw.<something>) of the audio file.
     * @param debug Enable/disable debug messages.
     * @return true, if audio file was succesfully played
     */
    public static boolean playAudio(Context context, int resourceID, boolean debug) {
        if (debug) {
            Log.d(TAG, "playAudio context: " + context + " resourceID: " + resourceID);
        }
        AudioPlayer ap = new AudioPlayer();
        boolean res = ap.play(context, resourceID);
        if (debug && !res)
        {
            Log.d(TAG, "playAudio failed");
        }

        return res;
    }

    public static boolean playAudio(Context context, int resourceID) {
        return playAudio(context, resourceID, false);
    }
}
