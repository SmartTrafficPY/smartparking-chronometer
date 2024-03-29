package smarttraffic.chronometer.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import smarttraffic.chronometer.Constants;
import smarttraffic.chronometer.R;
import smarttraffic.chronometer.activities.LoginActivity;
import smarttraffic.chronometer.services.RegistrationService;

/**
 * Created by Joaquin Olivera on july 19.
 *
 * @author joaquin
 */

public class RegistrationReceiver extends BroadcastReceiver {

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private String errorMessage;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(RegistrationService.REGISTRATION_OK)) {
            showToast(RegistrationService.REGISTRATION_OK, context);
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }else{
            setErrorMessage(intent.getStringExtra(RegistrationService.PROBLEM));
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            showToast(getErrorMessage(), context);
                        }
                    }, 8 * Constants.getSecondsInMilliseconds());
        }
    }
    // Show images in Toast prompt.
    @SuppressLint("ResourceAsColor")
    private void showToast(String message, Context context) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout toastContentView = (LinearLayout) toast.getView();
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.mipmap.smartparking_logo_round);
        toastContentView.addView(imageView, 0);
        toast.show();
    }
}
