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
    public void onboardDriver(String driverName, String phoneNumber, String licenceNumber,
                              String expireDate, String joiningDate, String aadharCard,
                              byte[] licencePhotoBytes, DriverCallback callback) {

        try {
            // Create temp file for photo
            File photoFile = File.createTempFile("licence", ".jpg");
            FileOutputStream fos = new FileOutputStream(photoFile);
            fos.write(licencePhotoBytes);
            fos.close();

            // Create multipart body parts
            RequestBody driverNameBody = RequestBody.create(MediaType.parse("text/plain"), driverName);
            RequestBody phoneBody = RequestBody.create(MediaType.parse("text/plain"), phoneNumber);
            RequestBody licenceBody = RequestBody.create(MediaType.parse("text/plain"), licenceNumber);
            RequestBody expireDateBody = RequestBody.create(MediaType.parse("text/plain"), expireDate);
            RequestBody joiningDateBody = RequestBody.create(MediaType.parse("text/plain"), joiningDate != null ? joiningDate : "");
            RequestBody aadharBody = RequestBody.create(MediaType.parse("text/plain"), aadharCard);

            RequestBody photoBody = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
            MultipartBody.Part photoPart = MultipartBody.Part.createFormData("licence_photo", photoFile.getName(), photoBody);

            String token = authManager.getBearerToken();

            Call<ResponseBody> call = apiService.onboardDriver(
                    token,
                    AppConfig.CLIENT_ID,
                    driverNameBody,
                    phoneBody,
                    licenceBody,
                    expireDateBody,
                    joiningDateBody,
                    aadharBody,
                    photoPart
            );

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    photoFile.delete(); // Clean up temp file

                    if (response.isSuccessful()) {
                        Log.d(TAG, "Driver onboarded successfully");
                        callback.onSuccess("Driver added successfully");
                    } else {
                        try {
                            String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Error: " + error);
                            callback.onError("Failed: " + response.code());
                        } catch (IOException e) {
                            callback.onError("Failed to add driver");
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    photoFile.delete(); // Clean up temp file
                    Log.e(TAG, "Network error", t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
            callback.onError("Error processing photo");
        }
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
