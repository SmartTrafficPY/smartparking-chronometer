package smarttraffic.chronometer.Interceptors;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;


/**
 * Created by Joaquin on 09/2019.
 * <p>
 * smarttraffic.smartparking.Interceptors
 */

public class ReceivedTimeStampInterceptor implements Interceptor {

    public static final String X_TIMESTAMP = "X-Timestamp";

    private Context context;

    public ReceivedTimeStampInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        SharedPreferences sharedPreferences = context.getSharedPreferences(X_TIMESTAMP,
                Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(X_TIMESTAMP, originalResponse.header(X_TIMESTAMP)).apply();
        editor.commit();

        return originalResponse;

    }
}
