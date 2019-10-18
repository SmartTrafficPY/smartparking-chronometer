package smarttraffic.chronometer.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import smarttraffic.chronometer.Constants;
import smarttraffic.chronometer.R;

/**
 * Created by Joaquin Olivera on july 19.
 *
 * @author joaquin
 */

public class InitActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(
                Constants.CLIENTE_DATA, Context.MODE_PRIVATE);

        try {
            // Google Play will install latest OpenSSL
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
                | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        final ProgressDialog progressDialog = new ProgressDialog(InitActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Inicializando aplicación...");
        progressDialog.show();
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        initializeFirstActivity(sharedPreferences);
                        progressDialog.dismiss();
                    }
                }, Constants.getSecondsInMilliseconds());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initializeFirstActivity(SharedPreferences sharedPreferences) {
        String userToken = sharedPreferences.getString(Constants.USER_TOKEN,
                Constants.CLIENT_NOT_LOGIN);
        if(userToken.equals(Constants.CLIENT_NOT_LOGIN)){
            Intent registration = new Intent(InitActivity.this,
                    BifurcationActivity.class);
            startActivity(registration);
        }else{
            Intent registration = new Intent(InitActivity.this,
                    HomeActivity.class);
            startActivity(registration);
        }
        finish();
    }

}

