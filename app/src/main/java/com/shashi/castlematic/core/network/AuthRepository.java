package com.shashi.castlematic.core.network;

import android.util.Log;
import com.shashi.castlematic.core.config.AppConfig;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.AuthResponse;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private final OkHttpClient client;

    public AuthRepository() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }

    public void login(String username, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                // Use the correct auth URL from config
                String authUrl = AppConfig.getCurrentAuthUrl() + AppConfig.AUTH_TOKEN_ENDPOINT;

                Log.d(TAG, "Login URL: " + authUrl);
                Log.d(TAG, "Client ID: " + AppConfig.CLIENT_ID);
                Log.d(TAG, "Username: " + username);

                // Build form-encoded request body matching your curl
                RequestBody formBody = new FormBody.Builder()
                        .add("grant_type", "password")
                        .add("username", username)
                        .add("password", password)
                        .add("scope", "")
                        .add("client_id", AppConfig.CLIENT_ID)
                        .add("client_secret", AppConfig.CLIENT_SECRET)
                        .build();

                Request request = new Request.Builder()
                        .url(authUrl)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .post(formBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + responseBody);

                if (response.isSuccessful()) {
                    // Parse the JSON response
                    AuthResponse authResponse = parseAuthResponse(responseBody);
                    callback.onSuccess(authResponse);
                } else {
                    callback.onError("Login failed: " + responseBody);
                }

            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }

    private AuthResponse parseAuthResponse(String json) {
        try {
            // Simple JSON parsing (you can use Gson if available)
            AuthResponse response = new AuthResponse();

            // Extract access_token
            int tokenStart = json.indexOf("\"access_token\":\"") + 16;
            int tokenEnd = json.indexOf("\"", tokenStart);
            response.accessToken = json.substring(tokenStart, tokenEnd);

            // Extract token_type
            int typeStart = json.indexOf("\"token_type\":\"") + 14;
            int typeEnd = json.indexOf("\"", typeStart);
            response.tokenType = json.substring(typeStart, typeEnd);

            Log.d(TAG, "Parsed token type: " + response.tokenType);

            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing auth response", e);
            AuthResponse response = new AuthResponse();
            response.accessToken = "parse_error";
            response.tokenType = "Bearer";
            return response;
        }
    }
}
