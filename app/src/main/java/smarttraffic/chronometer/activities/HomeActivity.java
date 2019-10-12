package smarttraffic.chronometer.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.BuildConfig;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import smarttraffic.chronometer.Constants;
import smarttraffic.chronometer.Interceptors.AddGeoJsonInterceptor;
import smarttraffic.chronometer.Interceptors.AddUserTokenInterceptor;
import smarttraffic.chronometer.Interceptors.ReceivedTimeStampInterceptor;
import smarttraffic.chronometer.R;
import smarttraffic.chronometer.SmartParkingAPI;
import smarttraffic.chronometer.StatesEnumerations;
import smarttraffic.chronometer.Utils;
import smarttraffic.chronometer.dataModels.Lots.Lot;
import smarttraffic.chronometer.dataModels.Lots.LotList;
import smarttraffic.chronometer.dataModels.Lots.LotProperties;
import smarttraffic.chronometer.dataModels.Lots.PointGeometry;
import smarttraffic.chronometer.dataModels.Spots.NearbySpot.NearbyLocation;
import smarttraffic.chronometer.dataModels.Point;
import smarttraffic.chronometer.dataModels.Spots.NearbySpot.NearbyPropertiesFeed;
import smarttraffic.chronometer.dataModels.Spots.Spot;
import smarttraffic.chronometer.dataModels.Spots.SpotList;
import smarttraffic.chronometer.dataModels.Spots.SpotProperties;
import smarttraffic.chronometer.receivers.AddAlarmReceiver;
import smarttraffic.chronometer.receivers.GeofenceBroadcastReceiver;
import smarttraffic.chronometer.receivers.RemoveAlarmReceiver;
import smarttraffic.chronometer.services.DetectedActivitiesService;
import smarttraffic.chronometer.services.GeofenceTransitionsJobIntentService;
import smarttraffic.chronometer.services.LocationUpdatesService;

import static smarttraffic.chronometer.Interceptors.ReceivedTimeStampInterceptor.X_TIMESTAMP;

/**
 * Created by Joaquin Olivera on july 19.
 *
 * @author joaquin
 */

public class HomeActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.buttonAbout)
    ImageButton buttonAbout;
    @BindView(R.id.timeCronometer)
    Chronometer chronometer;

    AddAlarmReceiver addAlarmReceiver = new AddAlarmReceiver();
    RemoveAlarmReceiver removeAlarmReceiver = new RemoveAlarmReceiver();

    private ActivityRecognitionClient mActivityRecognitionClient;

    int activityTransition;
    int geofenceTransition;
    int confidence;
    boolean userNotResponse = true;
    boolean dialogSendAllready = false;
    private Location mCurrentLocation;
    private List<Spot> spots = new ArrayList<Spot>();
    private ArrayList<String> geofencesTrigger = new ArrayList<>();
    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver geofenceReceiver;
    private BroadcastReceiver locationReceiver;
    private GeofencingClient geofencingClient;
    private PendingIntent mGeofencePendingIntent;
    final Handler handler = new Handler();
    Runnable cronJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        ButterKnife.bind(this);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);

        removeGeofences();
        Utils.geofencesSetUp(this, false);
        chronometer.setBase(SystemClock.elapsedRealtime());

        Utils.addAlarmsGeofencingTask(HomeActivity.this);

        buttonAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog settingsDialog = new Dialog(HomeActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View mView = inflater.inflate(R.layout.about_layout, null);
                final TextView username = (TextView) mView.findViewById(R.id.userName);
                final TextView version = (TextView) mView.findViewById(R.id.version_number);
                username.setText(Utils.getCurrentUsername(HomeActivity.this));
                version.setText(BuildConfig.VERSION_NAME);
                settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                settingsDialog.setContentView(mView);
                settingsDialog.show();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_TRANSITION_ACTIVITY_INTENT)) {
                    activityTransition = intent.getIntExtra(Constants.ACTIVITY_TYPE_TRANSITION, -1);
                    confidence = intent.getIntExtra(Constants.ACTIVITY_CONFIDENCE_TRANSITION, -1);
                }
            }
        };

        geofenceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.getBroadcastGeofenceTriggerIntent())) {
                    geofencesTrigger = intent.getStringArrayListExtra(
                            Constants.GEOFENCE_TRIGGED);
                    geofenceTransition = intent.getIntExtra(
                            GeofenceTransitionsJobIntentService.TRANSITION,
                            -1);
                    managerOfTransitions();
                }
            }
        };

        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCurrentLocation = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
                if (mCurrentLocation != null) {
                    checkForUserLocation(mCurrentLocation);
                }
            }
        };
        setSupportActionBar(toolbar);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void managerOfTransitions() {
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                Constants.SETTINGS, MODE_PRIVATE);
        getSpotsFromGeofence(geofencesTrigger, false);
        final long delay = sharedPreferences.getLong(Constants.MAP_SPOTS_TIME_UPDATE_SETTINGS,
                Constants.getSecondsInMilliseconds() * 45);
        cronJob = new Runnable() {
            public void run() {
                getSpotsFromGeofence(geofencesTrigger, true);
                handler.postDelayed(this, delay);
            }
        };
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                handler.postDelayed(cronJob, delay);
                requestActivityUpdates();
                chronometer.start();
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                if(Utils.returnEnterLotFlag(this)){
                    Utils.hasEnterLotFlag(this, false);
                    Utils.setEntranceEvent(this, mCurrentLocation, Constants.EVENT_TYPE_EXIT);
                }
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                handler.removeCallbacks(cronJob);
                break;
            default:
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                removeActivityUpdates();
                handler.removeCallbacks(cronJob);
                break;
        }
    }

    private void getSpotsFromGeofence(ArrayList<String> geofencesTrigger, boolean isForUpdate) {
        if(geofencesTrigger != null) {
            if(isForUpdate){
                updatesSpotsFromGeofence();
            }else{
                for (String geofenceTrigger : geofencesTrigger) {
                    getSpotsGeographicValues(geofenceTrigger);
                }
            }
        }
    }

    private void getSpotsGeographicValues(String geofencesTrigger) {
        final SharedPreferences sharedPreferences = this.getSharedPreferences(Constants.SETTINGS,
                MODE_PRIVATE);
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AddGeoJsonInterceptor())
                .addInterceptor(new AddUserTokenInterceptor(this))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        int lotId = Utils.getLotInSharedPreferences(HomeActivity.this, geofencesTrigger);
        SmartParkingAPI smartParkingAPI = retrofit.create(SmartParkingAPI.class);
        Call<SpotList> call = smartParkingAPI.getAllGeoJsonSpotsInLot(lotId);

        call.enqueue(new Callback<SpotList>() {
            @Override
            public void onResponse(Call<SpotList> call, Response<SpotList> response) {
                switch (response.code()) {
                    case 200:
                        SpotList testSpots = response.body();
                        if(!testSpots.isEmpty()){
                            spots = testSpots.getFeatures();
                        }
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onFailure(Call<SpotList> call, Throwable t) {
                if(!Utils.isInternetAvailable()){
                    Utils.showToast(Constants.CONNECTION_FAILED, HomeActivity.this);
                }
                t.printStackTrace();
            }
        });
    }

    private void updatesSpotsFromGeofence() {
        final SharedPreferences preferencesSettings = this.getSharedPreferences(Constants.SETTINGS,
                MODE_PRIVATE);
        SharedPreferences preferencesTimestamp = this.getSharedPreferences(
                X_TIMESTAMP,MODE_PRIVATE);
        NearbyLocation nearbyLocation = new NearbyLocation();
        PointGeometry point = new PointGeometry();
        NearbyPropertiesFeed nearbyPropertiesFeed = new NearbyPropertiesFeed();

        if(mCurrentLocation != null){
            point.setPointCoordinates(mCurrentLocation);
        }
        nearbyPropertiesFeed.setPrevious_timestamp(preferencesTimestamp.getString(
                X_TIMESTAMP,"1559447999"));
        nearbyLocation.setGeometry(point);
        nearbyLocation.setProperties(nearbyPropertiesFeed);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new ReceivedTimeStampInterceptor(this))
                .addInterceptor(new AddUserTokenInterceptor(this))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        SmartParkingAPI smartParkingAPI = retrofit.create(SmartParkingAPI.class);
        Call<HashMap<String, String>> call = smartParkingAPI.getNearbySpots(
                "application/vnd.geo+json", nearbyLocation);

        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                switch (response.code()) {
                    case 200:
                        HashMap<String, String> changedSpots = response.body();
                        List<Spot> spotsUpdated = Utils.updateSpots(changedSpots, spots);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                if(!Utils.isInternetAvailable()){
                    Utils.showToast(Constants.CONNECTION_FAILED, HomeActivity.this);
                }
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(Utils.isDayOfWeek() && !Utils.getGeofenceStatus(HomeActivity.this)){
            addParkingLotsGeofences();
        }
        if (!checkPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeActivityUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_TRANSITION_ACTIVITY_INTENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(geofenceReceiver,
                new IntentFilter(Constants.getBroadcastGeofenceTriggerIntent()));
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        registerReceiver(addAlarmReceiver, new IntentFilter());
        registerReceiver(removeAlarmReceiver, new IntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(geofenceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
        unregisterReceiver(addAlarmReceiver);
        unregisterReceiver(removeAlarmReceiver);
        removeActivityUpdates();
    }

    private void addParkingLotsGeofences() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AddUserTokenInterceptor(this))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        SmartParkingAPI smartParkingAPI = retrofit.create(SmartParkingAPI.class);
        Call<LotList> call = smartParkingAPI.getAllLots();

        call.enqueue(new Callback<LotList>() {
            @Override
            public void onResponse(Call<LotList> call, Response<LotList> response) {
                switch (response.code()) {
                    case 200:
                        List<Lot> lots = response.body().getFeatures();
                        Utils.saveLotInSharedPreferences(HomeActivity.this, lots);
                        ArrayList<Geofence> geofenceList = new ArrayList<>();
                        for (Lot lot : lots) {
                            LotProperties properties = lot.getProperties();
                            Point center = properties.getCenter().getPointCoordinates();
                            geofenceList.add(generateGeofence(center.getLatitud(),
                                    center.getLongitud(),
                                    properties.getRadio(),
                                    properties.getName(), false));
                        }
                        addGeofences(geofenceList);
                        Utils.saveListOfGateways(HomeActivity.this, response.body());
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onFailure(Call<LotList> call, Throwable t) {
                if(!Utils.isInternetAvailable()){
                    Utils.showToast(Constants.CONNECTION_FAILED, HomeActivity.this);
                }
                t.printStackTrace();
            }
        });
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    private Geofence generateGeofence(double latitude, double longitud, float radius, String nameId,
                                      boolean isSpotGeofence) {
        Geofence.Builder builder = new Geofence.Builder()
                .setRequestId(nameId)
                .setCircularRegion(
                        latitude,
                        longitud,
                        radius
                )
                .setLoiteringDelay(1000 * 60 * 20)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT |
                        Geofence.GEOFENCE_TRANSITION_DWELL);
        if(isSpotGeofence){
            builder.setExpirationDuration(Constants.getHoursInMilliseconds() * 24);
        }else{
            builder.setExpirationDuration(Geofence.NEVER_EXPIRE);
        }
        Geofence geofence = builder.build();
        return geofence;
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                    findViewById(R.id.home_layout),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.button_accept, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(HomeActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    Constants.getRequestPermissionsRequestCode());
                        }
                    })
                    .show();
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.getRequestPermissionsRequestCode());
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == Constants.getRequestPermissionsRequestCode()) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
            } else {
                // Permission denied.
                Snackbar.make(
                        findViewById(R.id.home_layout),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences(ArrayList<Geofence> geofenceArrayList) {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }
        geofencingClient.addGeofences(getGeofencingRequest(geofenceArrayList), getGeofencePendingIntent());
        Utils.geofencesSetUp(HomeActivity.this,true);
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    public void removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }
        geofencingClient.removeGeofences(getGeofencePendingIntent());
        Utils.geofencesSetUp(HomeActivity.this,false);
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);

        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest(ArrayList<Geofence> geofenceList) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private GeofencingRequest getGeofenceRequest(Spot spot) {
        List<Point> points = spot.getGeometry().getPolygonPoints();
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(generateGeofence(points.get(0).getLatitud(), points.get(0).getLongitud(),
                50, "Tu vehiculo en " + spot.getProperties().getIdFromUrl(), true));
        return builder.build();
    }

    private void checkForUserLocation(Location mCurrentLocation) {
        int spotId = isPointInsideParkingSpot(spots, mCurrentLocation);
        if (spotId != Constants.NOT_IN_PARKINGSPOT) {
            Spot spot = getSpotFromId(spots, spotId);
            SpotProperties spotProperties = spot.getProperties();
            if (!spotProperties.getState().equals(StatesEnumerations.OCCUPIED.getEstado())){
                if(!(activityTransition == DetectedActivity.RUNNING ||
                        activityTransition == DetectedActivity.ON_FOOT ||
                        activityTransition == DetectedActivity.WALKING) && !dialogSendAllready){
                    confirmationOfActionDialog(spotId, true);
                }
            } else {
                if(!dialogSendAllready){
                    confirmationOfActionDialog(spotId, false);
                }
            }
        }
    }

    private Spot getSpotFromId(List<Spot> spots, int spotId) {
        Spot result = new Spot();
        for (Spot spot : spots) {
            if (spot.getProperties().getIdFromUrl() == spotId) {
                result = spot;
            }
        }
        return result;
    }

    /**
     * Show a custom_report_dialog tha could be:
     * OCCUPYING a spot OR FREEING ONE
     * **/
    @SuppressWarnings("MissingPermission")
    private void confirmationOfActionDialog(final int spotIdIn, final boolean isParking) {
        final AlertDialog.Builder builder;
        if (isParking) {
            final AlertDialog.Builder ocupationBuilder = new AlertDialog.Builder(this,
                    R.style.AppTheme_SmartParking_DialogOccupation);
            ocupationBuilder.setMessage(R.string.are_you_parking)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Utils.setNewStateOnSpot(HomeActivity.this, isParking, spotIdIn);
                                userNotResponse = false;
                                final Timer geofencetimer = new Timer();
                                Utils.changeStatusOfSpot(spotIdIn, spots, "O");
                                geofencetimer.schedule(new TimerTask() {
                                    public void run() {
                                        geofencingClient.addGeofences(getGeofenceRequest(
                                                getSpotFromId(spots, spotIdIn)),
                                                getGeofencePendingIntent());
                                    }
                                }, Constants.getMinutesInMilliseconds() * 5);
                                Intent serviceIntent = new Intent(HomeActivity.this,
                                        LocationUpdatesService.class);
                                stopService(serviceIntent);
                                chronometer.stop();
                                chronometer.setBase(SystemClock.elapsedRealtime());
                            }
                        });
            ocupationBuilder.setNegativeButton(R.string.not_get_spot_occupied, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Utils.setNewStateOnSpot(HomeActivity.this, false, spotIdIn);
                    Utils.changeStatusOfSpot(spotIdIn, spots, "F");
                    userNotResponse = false;
                    List<String> geofencesToRemove = new ArrayList<>();
                    geofencesToRemove.add("Tu vehiculo en " + spotIdIn);
                    geofencingClient.removeGeofences(geofencesToRemove);
                }
            });
            ocupationBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        userNotResponse = false;
                        dialog.dismiss();
                    }
                });
            builder = ocupationBuilder;
        }else{
            final AlertDialog.Builder freeBuilder = new AlertDialog.Builder(this,
                    R.style.AppTheme_SmartParking_DialogLiberation);
            freeBuilder.setMessage(R.string.are_you_vacating_a_place)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utils.setNewStateOnSpot(HomeActivity.this, isParking, spotIdIn);
                        Utils.changeStatusOfSpot(spotIdIn, spots, "F");
                        userNotResponse = false;
                        List<String> geofencesToRemove = new ArrayList<>();
                        geofencesToRemove.add("Tu vehiculo en " + spotIdIn);
                        geofencingClient.removeGeofences(geofencesToRemove);
                    }
                });
            freeBuilder.setNegativeButton(R.string.not_get_spot_free, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Utils.setNewStateOnSpot(HomeActivity.this, true, spotIdIn);
                    final Timer geofencetimer = new Timer();
                    geofencetimer.schedule(new TimerTask() {
                        public void run() {
                            geofencingClient.addGeofences(getGeofenceRequest(
                                    getSpotFromId(spots, spotIdIn)),
                                    getGeofencePendingIntent());
                        }
                    }, Constants.getMinutesInMilliseconds() * 5);
                    Intent serviceIntent = new Intent(HomeActivity.this,
                            LocationUpdatesService.class);
                    stopService(serviceIntent);
                    userNotResponse = false;
                    chronometer.stop();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                }
            });
            freeBuilder.setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userNotResponse = false;
                    dialog.dismiss();
                }
            });
            builder = freeBuilder;
        }
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        dialogSendAllready = true;
        final Timer dialogtimer = new Timer();
        dialogtimer.schedule(new TimerTask() {
            public void run() {
                alertDialog.dismiss();
                if(userNotResponse){
                    Utils.setNewStateOnSpot(HomeActivity.this, isParking, spotIdIn);
                }
                userNotResponse = true;
            }
        }, Constants.getSecondsInMilliseconds() * 20);
        dialogtimer.schedule(new TimerTask() {
            public void run() {
                dialogSendAllready = false;
                dialogtimer.cancel();
            }
        }, Constants.getSecondsInMilliseconds() * 30);
    }

    public boolean isPointInsidePolygon(Spot spot, Location location){
        return PolyUtil.containsLocation(location.getLatitude(),location.getLongitude(),spot.toLatLngList(),
                true);
    }

    public int isPointInsideParkingSpot(List<Spot> ParkingSpot, Location location){
        for(Spot spot : ParkingSpot){
            if (isPointInsidePolygon(spot, location)){
                return spot.getProperties().getIdFromUrl();
            }
        }
        return Constants.NOT_IN_PARKINGSPOT;
    }

    /**
     * Registers for activity recognition updates using
     * {@link ActivityRecognitionClient#requestActivityUpdates(long, PendingIntent)}.
     * Registers success and failure callbacks.
     */
    public void requestActivityUpdates() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent());

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                setUpdatesRequestedState(true);
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                setUpdatesRequestedState(false);
            }
        });
    }
    /**
     * Removes activity recognition updates using
     * {@link ActivityRecognitionClient#removeActivityUpdates(PendingIntent)}. Registers success and
     * failure callbacks.
     */
    public void removeActivityUpdates() {
        @SuppressLint("MissingPermission")
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                setUpdatesRequestedState(false);
                // Reset the display.
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                setUpdatesRequestedState(true);
            }
        });
    }
    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requesting) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, requesting)
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(
                Constants.CLIENTE_DATA, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_changepass) {
            Intent changePassIntent = new Intent(HomeActivity.this, ChangePasswordActivity.class);
            startActivity(changePassIntent);
            return true;
        }else if(id == R.id.menu_logout){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.are_you_sure_logout)
                    .setPositiveButton(R.string.button_accept, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            editor.putString(Constants.USER_TOKEN, Constants.CLIENT_NOT_LOGIN).apply();
                            editor.commit();
                            Intent logoutIntent = new Intent(HomeActivity.this,
                                    BifurcationActivity.class);
                            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(logoutIntent);
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the custom_report_dialog
                        }
                    });
            builder.create().show();
            return true;
        }else if(id == R.id.setting_menu){
            Intent settingsActivity = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(settingsActivity);
        }else if(id == R.id.report_menu){
            messageDialogReport();
        }
        return super.onOptionsItemSelected(item);
    }

    public void messageDialogReport(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View mview = inflater.inflate(R.layout.custom_report_dialog, null);
        final EditText text = (EditText) mview.findViewById(R.id.message_of_report);
        builder.setView(mview);
        // Set up the input
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMessageReport(text.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void sendMessageReport(String message){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        Intent chooser = Intent.createChooser(sendIntent, "Send bug report");
        startActivity(chooser);
    }

}
