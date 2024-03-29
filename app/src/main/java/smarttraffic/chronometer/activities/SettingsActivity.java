package smarttraffic.chronometer.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import smarttraffic.chronometer.Constants;
import smarttraffic.chronometer.R;
import smarttraffic.chronometer.Utils;

/**
 * Created by Joaquin on 09/2019.
 * <p>
 * smarttraffic.smartparking.activities
 */

public class SettingsActivity extends Activity {

    @BindView(R.id.saveSettings)
    Button saveSettings;
    //Location Ubication...
    @BindView(R.id.fastTimeGPS)
    RadioButton fastTimeGPS;
    @BindView(R.id.normalTimeGPS)
    RadioButton normalTimeGPS;
    @BindView(R.id.slowTimeGPS)
    RadioButton slowTimeGPS;
    @BindView(R.id.gpsActualizationsTime)
    RadioGroup gpsActualizationsTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        ButterKnife.bind(this);

        SharedPreferences sharedPreferences = this.getSharedPreferences(
                Constants.SETTINGS, MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        
        checkValuesFromSettingsOptions(setSettingInfo(sharedPreferences));

        saveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkValuesFromSettingsOptions(saveAllNewSettings(editor));
                Utils.showToast(SettingsActivity.this.getResources().getString(R.string.new_configurations_accepted),
                        SettingsActivity.this);
                Utils.settingsHasChanged(SettingsActivity.this);
                finish();
            }
        });

    }

    private HashMap<String, String> saveAllNewSettings(SharedPreferences.Editor editor) {
        editor.putLong(Constants.LOCATION_TIME_UPDATE_SETTINGS, onRadioLocationClicked()).apply();
        editor.commit();
        return returnNewSettings();
    }

    private HashMap<String, String> returnNewSettings() {
        HashMap<String,String> newSettings = new HashMap<>();
        newSettings.put(Constants.LOCATION_TIME_UPDATE_SETTINGS, String.valueOf(onRadioLocationClicked()));
        return  newSettings;
    }

    private void checkValuesFromSettingsOptions(HashMap<String, String> settings) {
        checkForLocationUpdate(settings);
    }

    private HashMap<String, String> setSettingInfo(SharedPreferences sharedPreferences) {
        HashMap<String,String> options = new HashMap<>();
        options.put(Constants.LOCATION_TIME_UPDATE_SETTINGS, String.valueOf(sharedPreferences.getLong(
                Constants.LOCATION_TIME_UPDATE_SETTINGS, Constants.getSecondsInMilliseconds() * 5)));
        options.put(Constants.MAP_SPOTS_TIME_UPDATE_SETTINGS, String.valueOf(sharedPreferences.getLong(
                Constants.MAP_SPOTS_TIME_UPDATE_SETTINGS,
                Constants.getSecondsInMilliseconds() * 45)));
        options.put(Constants.DRAW_SETTINGS,sharedPreferences.getString(
                Constants.DRAW_SETTINGS, Constants.POLYGON_TO_DRAW_SETTINGS));
        return options;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public Long onRadioLocationClicked() {
        if(fastTimeGPS.isChecked()){
            return Constants.getSecondsInMilliseconds();
        }else if(slowTimeGPS.isChecked()){
            return Constants.getSecondsInMilliseconds() * 10;
        }else{
            return Constants.getSecondsInMilliseconds() * 5;
        }
    }

    private void checkForLocationUpdate(HashMap<String, String> settings){
        if(settings.get(Constants.LOCATION_TIME_UPDATE_SETTINGS) != null){
            if(Long.valueOf(settings.get(Constants.LOCATION_TIME_UPDATE_SETTINGS)) ==
                    Constants.getSecondsInMilliseconds()){
                gpsActualizationsTime.check(R.id.fastTimeGPS);

            }else if(Long.valueOf(settings.get(Constants.LOCATION_TIME_UPDATE_SETTINGS))==
                    Constants.getSecondsInMilliseconds() * 5){
                gpsActualizationsTime.check(R.id.normalTimeGPS);

            }else{
                gpsActualizationsTime.check(R.id.slowTimeGPS);

            }
        }

    }

}
