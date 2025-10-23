package com.shashi.castlematic.core.network;

import android.util.Log;

import com.shashi.castlematic.features.fuel_theft.models.TheftModels.AuthResponse;
import com.shashi.castlematic.core.config.AppConfig;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AuthRepository {
    private static final String TAG = "AuthRepository";

    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }

    public void login(String username, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                FormBody formBody = new FormBody.Builder()
                        .add("grant_type", "password")
                        .add("username", username)
                        .add("password", password)
                        .add("scope", "")
                        .add("client_id", AppConfig.CLIENT_ID)
                        .add("client_secret", AppConfig.CLIENT_SECRET)
                        .build();

                Request request = new Request.Builder()
                        .url(AppConfig.AUTH_BASE_URL + "/auth/token")
                        .post(formBody)
                        .addHeader("accept", "application/json")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();

                Log.d(TAG, "Sending login request to: " + request.url());

                Response response = client.newCall(request).execute();
                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response body: " + responseBody);

                    // Manual JSON parsing to ensure role is captured
                    try {
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);

                        AuthResponse authResponse = new AuthResponse();
                        authResponse.accessToken = jsonResponse.getString("access_token");
                        authResponse.tokenType = jsonResponse.getString("token_type");
                        authResponse.role = jsonResponse.optString("role", "admin");

                        if (jsonResponse.has("expires_in")) {
                            authResponse.expiresIn = jsonResponse.getLong("expires_in");
                        }

                        Log.d(TAG, "Parsed - Token: " + authResponse.tokenType + ", Role: " + authResponse.role);

                        callback.onSuccess(authResponse);

                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }

                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Login failed - Code: " + response.code() + ", Body: " + errorBody);
                    callback.onError("Login failed: " + response.code());
                }

            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                if (e.getMessage() != null &&
                        (e.getMessage().contains("CLEARTEXT") ||
                                e.getMessage().contains("ERR_NGROK_") ||
                                e.getMessage().contains("Unable to resolve host"))) {
                    callback.onError("Server offline or unreachable");
                } else {
                    callback.onError("Network error: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
}
