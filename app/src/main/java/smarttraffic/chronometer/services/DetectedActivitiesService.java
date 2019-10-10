package smarttraffic.chronometer.services;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import smarttraffic.chronometer.Constants;
import smarttraffic.chronometer.Utils;

public class DetectedActivitiesService extends IntentService {

    public DetectedActivitiesService() {
        super("DetectedActivitiesService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.KEY_DETECTED_ACTIVITIES,
                        Utils.detectedActivitiesToJson(detectedActivities))
                .apply();

        broadcastActivityTransition(result);
    }

    private void broadcastActivityTransition(ActivityRecognitionResult result) {
        Intent intent = new Intent(Constants.BROADCAST_TRANSITION_ACTIVITY_INTENT);
        intent.putExtra(Constants.ACTIVITY_TYPE_TRANSITION, result.getMostProbableActivity().getType());
        intent.putExtra(Constants.ACTIVITY_CONFIDENCE_TRANSITION, result.getMostProbableActivity().getConfidence());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
