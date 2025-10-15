package com.shashi.castlematic.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    private static AuthManager instance;
    private final SharedPreferences preferences;

    private AuthManager(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // Save authentication token
    public void saveToken(String accessToken, String tokenType) {
        Log.d(TAG, "Saving token - Type: " + tokenType);
        preferences.edit()
                .putString(KEY_TOKEN, accessToken)
                .putString(KEY_TOKEN_TYPE, tokenType)
                .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000)) // 24 hours
                .apply();
    }

    // Get access token
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    // Get token type
    public String getTokenType() {
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, "Bearer");
        // Ensure first letter is uppercase
        if (tokenType != null && !tokenType.isEmpty()) {
            return tokenType.substring(0, 1).toUpperCase() + tokenType.substring(1).toLowerCase();
        }
        return "Bearer";
    }

    // Get bearer token (combines token type and token) - ADD THIS!
    public String getBearerToken() {
        String token = getToken();
        String tokenType = getTokenType();

        if (token != null && !token.isEmpty()) {
            return tokenType + " " + token;
        }
        return null;
    }

    // Check if token exists and is valid
    public boolean hasValidToken() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            Log.d(TAG, "No token found");
            return false;
        }

        long expiry = preferences.getLong(KEY_TOKEN_EXPIRY, 0);
        boolean isExpired = System.currentTimeMillis() > expiry;

        if (isExpired) {
            Log.d(TAG, "Token expired");
            clearToken();
            return false;
        }

        Log.d(TAG, "Valid token found");
        return true;
    }

    // Clear authentication token
    public void clearToken() {
        Log.d(TAG, "Clearing token");
        preferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_TOKEN_TYPE)
                .remove(KEY_TOKEN_EXPIRY)
                .apply();
    }

    // Get authorization header value (same as getBearerToken, for compatibility)
    public String getAuthorizationHeader() {
        return getBearerToken();
    }
}
