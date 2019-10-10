package smarttraffic.chronometer.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import smarttraffic.chronometer.R;
import smarttraffic.chronometer.activities.HomeActivity;
import smarttraffic.chronometer.services.LoginService;

/**
 * Created by Joaquin Olivera on july 19.
 *
 * @author joaquin
 */

public class LoginReceiver extends BroadcastReceiver {

    private String sex;
    private Integer age;
    private Integer identifier;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(LoginService.LOGIN_ACTION)) {
            Intent i = new Intent(context, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }else{
            setErrorMessage(intent.getStringExtra(LoginService.PROBLEM));
            showToast(getErrorMessage(),context);
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private String errorMessage;

    public String getSexResponse() {
        return sex;
    }

    public void setSexResponse(String sexResponse) {
        this.sex = sexResponse;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
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
