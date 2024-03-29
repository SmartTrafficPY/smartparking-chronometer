package smarttraffic.chronometer;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import smarttraffic.chronometer.dataModels.AppToken;
import smarttraffic.chronometer.dataModels.Credentials;
import smarttraffic.chronometer.dataModels.Events;
import smarttraffic.chronometer.dataModels.Lots.LotList;
import smarttraffic.chronometer.dataModels.Spots.NearbySpot.NearbyLocation;
import smarttraffic.chronometer.dataModels.ProfileUser;
import smarttraffic.chronometer.dataModels.ProfileRegistry;
import smarttraffic.chronometer.dataModels.Spots.NearbySpot.NearbySpotList;
import smarttraffic.chronometer.dataModels.Spots.SpotList;
import smarttraffic.chronometer.dataModels.UserToken;

public interface SmartParkingAPI {

    /**USERS**/
    @POST("smartparking/auth-token/")
    Call<UserToken> getUserToken(@Body Credentials userCredentials);

    @POST("smartparking/users/")
    Call<ProfileUser> signUpUser(@Body ProfileRegistry profileRegistry);

    @PATCH("smartparking/users/{identifier}/")
    Call<ProfileUser> updateUserProfile(@Path("identifier") Integer userId,
                                        @Body Credentials newProfile);

    /**SPOTS**/
    @POST("smartparking/spots/{spotId}/reset/")
    Call<ResponseBody> resetFreeSpot(@Header("Content-Type") String content_type,
                                     @Path("spotId") Integer spotId,
                                     @Body AppToken appToken);

    @POST("smartparking/spots/{spotId}/set/")
    Call<ResponseBody> setOccupiedSpot(@Header("Content-Type") String content_type,
                                       @Path("spotId") Integer spotId,
                                       @Body AppToken appToken);

    @POST("smartparking/spots/nearby/")
    Call<HashMap<String, String>> getMapNearbySpots(@Body NearbyLocation nearbyLocation);

    @POST("smartparking/spots/nearby/")
    Call<NearbySpotList> getGeoJsonNearbySpots(@Header("Content-Type") String content_type,
                                               @Header("Accept") String accept,
                                               @Body NearbyLocation nearbyLocation);

    @POST("smartparking/spots/nearby/")
    Call<HashMap<String, String>> getNearbySpots(@Header("Content-Type") String content_type,
                                               @Body NearbyLocation nearbyLocation);

    /**LOTS**/
    @GET("smartparking/lots/")
    Call<LotList> getAllLots();

    @GET("smartparking/lots/{lotId}/spots/")
    Call<HashMap<String, String>> getAllMapSpotsInLot(@Path("lotId") Integer lotId);

    @GET("smartparking/lots/{lotId}/spots/")
    Call<SpotList> getAllGeoJsonSpotsInLot(@Path("lotId") Integer lotId);

    /**EVENTS**/
    @POST("events/")
    Call<ResponseBody> setUserEvent(@Header("Content-Type") String content_type, @Body Events event);
}
