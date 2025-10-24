package com.shashi.castlematic.features.user_management.repository;

import android.content.Context;
import android.util.Log;

import com.shashi.castlematic.core.config.AppConfig;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.features.user_management.api.UserManagementApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserManagementRepository {

    private static final String TAG = "UserMgmtRepository";
    private final UserManagementApiService apiService;
    private final AuthManager authManager;

    public UserManagementRepository(Context context) {
        this.authManager = AuthManager.getInstance(context);

        OkHttpClient client = new OkHttpClient.Builder().build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.analytics.castlematic.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(UserManagementApiService.class);
    }

    // Onboard Driver
    // Onboard Driver with 3 photos
    public void onboardDriver(String driverName, String phoneNumber, String licenceNumber,
                              String expireDate, String joiningDate, String aadharCard,
                              byte[] licencePhotoBytes, byte[] licencePhotoBackBytes, byte[] driverPhotoBytes,
                              String userId, String userPassword,
                              DriverCallback callback) {

        try {
            // Create temp files for photos
            File photoFront = File.createTempFile("licence_front", ".jpg");
            FileOutputStream fosFront = new FileOutputStream(photoFront);
            fosFront.write(licencePhotoBytes);
            fosFront.close();

            File photoBack = File.createTempFile("licence_back", ".jpg");
            FileOutputStream fosBack = new FileOutputStream(photoBack);
            fosBack.write(licencePhotoBackBytes);
            fosBack.close();

            File photoDriver = File.createTempFile("driver_photo", ".jpg");
            FileOutputStream fosDriver = new FileOutputStream(photoDriver);
            fosDriver.write(driverPhotoBytes);
            fosDriver.close();

            // Create multipart body parts
            RequestBody driverNameBody = RequestBody.create(MediaType.parse("text/plain"), driverName);
            RequestBody phoneBody = RequestBody.create(MediaType.parse("text/plain"), phoneNumber);
            RequestBody licenceBody = RequestBody.create(MediaType.parse("text/plain"), licenceNumber);
            RequestBody expireDateBody = RequestBody.create(MediaType.parse("text/plain"), expireDate);
            RequestBody joiningDateBody = RequestBody.create(MediaType.parse("text/plain"), joiningDate != null ? joiningDate : "");
            RequestBody aadharBody = RequestBody.create(MediaType.parse("text/plain"), aadharCard);

            RequestBody photoFrontBody = RequestBody.create(MediaType.parse("image/jpeg"), photoFront);
            MultipartBody.Part photoFrontPart = MultipartBody.Part.createFormData("licence_photo", photoFront.getName(), photoFrontBody);

            RequestBody photoBackBody = RequestBody.create(MediaType.parse("image/jpeg"), photoBack);
            MultipartBody.Part photoBackPart = MultipartBody.Part.createFormData("licence_photo_back", photoBack.getName(), photoBackBody);

            RequestBody photoDriverBody = RequestBody.create(MediaType.parse("image/jpeg"), photoDriver);
            MultipartBody.Part photoDriverPart = MultipartBody.Part.createFormData("driver_photo", photoDriver.getName(), photoDriverBody);

            String token = authManager.getBearerToken();

            Log.d(TAG, "Step 1: Calling driver onboard API");

            // STEP 1: Onboard Driver
            Call<ResponseBody> driverCall = apiService.onboardDriver(
                    token,
                    AppConfig.CLIENT_ID,
                    driverNameBody,
                    phoneBody,
                    licenceBody,
                    expireDateBody,
                    joiningDateBody,
                    aadharBody,
                    photoFrontPart,
                    photoBackPart,
                    photoDriverPart
            );

            driverCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    // Clean up temp files
                    photoFront.delete();
                    photoBack.delete();
                    photoDriver.delete();

                    if (response.isSuccessful()) {
                        Log.d(TAG, "Step 1 Success: Driver onboarded");

                        // STEP 2: Create user login credentials
                        Log.d(TAG, "Step 2: Creating user login with userId: " + userId);
                        onboardUserForDriver(userId, driverName, userPassword, callback);

                    } else {
                        try {
                            String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Driver onboard error " + response.code() + ": " + error);
                            callback.onError("Failed to add driver: " + response.code());
                        } catch (IOException e) {
                            callback.onError("Failed to add driver");
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    photoFront.delete();
                    photoBack.delete();
                    photoDriver.delete();
                    Log.e(TAG, "Network error in driver onboard", t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error creating temp files", e);
            callback.onError("Error processing photos");
        }
    }

    // Helper method to onboard user after driver is created
    private void onboardUserForDriver(String userId, String userName, String userPassword, DriverCallback callback) {
        UserManagementApiService.OnboardUserRequest request =
                new UserManagementApiService.OnboardUserRequest(
                        userId, userName, "driver", AppConfig.CLIENT_ID, userPassword
                );

        String token = authManager.getBearerToken();

        Call<ResponseBody> userCall = apiService.onboardUser(token, request);

        userCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Step 2 Success: User login created for driver");
                    callback.onSuccess("Driver and login credentials added successfully!");
                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "User onboard error " + response.code() + ": " + error);
                        callback.onError("Driver added but login creation failed: " + response.code());
                    } catch (IOException e) {
                        callback.onError("Driver added but login creation failed");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network error in user onboard", t);
                callback.onError("Driver added but login creation failed: " + t.getMessage());
            }
        });
    }

    // Onboard User
    public void onboardUser(String userId, String userName, String userRole,
                            String userPassword, UserCallback callback) {

        UserManagementApiService.OnboardUserRequest request =
                new UserManagementApiService.OnboardUserRequest(
                        userId, userName, userRole, AppConfig.CLIENT_ID, userPassword
                );

        String token = authManager.getBearerToken();

        Call<ResponseBody> call = apiService.onboardUser(token, request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User onboarded successfully");
                    callback.onSuccess("User added successfully");
                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error: " + error);
                        callback.onError("Failed: " + response.code());
                    } catch (IOException e) {
                        callback.onError("Failed to add user");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Callbacks
    public interface DriverCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface UserCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
