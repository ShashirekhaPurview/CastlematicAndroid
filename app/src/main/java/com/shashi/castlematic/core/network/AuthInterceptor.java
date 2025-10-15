package com.shashi.castlematic.core.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.shashi.castlematic.core.config.AppConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final Context context;

    // Different client IDs for different purposes
    private static final String AUTH_CLIENT_ID = "MEIL-5435"; // For auth/token endpoint
    private static final String API_CLIENT_ID = "MEIL-5345";  // For fuel-theft and other APIs

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // Get the token from AuthManager
        AuthManager authManager = AuthManager.getInstance(context);
        String token = authManager.getToken();
        String tokenType = authManager.getTokenType();

        // Add Authorization header if token exists
        if (token != null && !token.isEmpty()) {
            String authHeader = tokenType + " " + token;
            requestBuilder.addHeader("Authorization", authHeader);
            Log.d(TAG, "Added Authorization header: " + tokenType + " [token]");
        }

        // CRITICAL: Add client-id header for API endpoints
        String url = originalRequest.url().toString();
        if (url.contains("/fuel-theft") ||
                url.contains("/vehicle-idling-hours") ||
                url.contains("/vehicle-idle-reason") ||
                url.contains("/api/")) {
            requestBuilder.addHeader("client-id", API_CLIENT_ID);
            Log.d(TAG, "Added client-id header: " + API_CLIENT_ID + " for URL: " + url);
        }

        // Add other common headers
        requestBuilder.addHeader("accept", "application/json");

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}
