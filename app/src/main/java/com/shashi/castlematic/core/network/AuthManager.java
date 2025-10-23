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
    private static final String KEY_USER_ROLE = "user_role";  // NEW

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

    // Save authentication token and role
    // In AuthManager.java
    public void saveToken(String accessToken, String tokenType, String role) {
        Log.d(TAG, "Saving token - Type: " + tokenType + ", Role: " + role);
        preferences.edit()
                .putString(KEY_TOKEN, accessToken)
                .putString(KEY_TOKEN_TYPE, tokenType)
                .putString(KEY_USER_ROLE, role != null ? role : "admin")
                .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000))
                .apply();
    }

    // Get access token
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    // Get token type
    public String getTokenType() {
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, "Bearer");
        if (tokenType != null && !tokenType.isEmpty()) {
            return tokenType.substring(0, 1).toUpperCase() + tokenType.substring(1).toLowerCase();
        }
        return "Bearer";
    }

    // NEW: Get user role
    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, "user");
    }

    // NEW: Check if user is admin
    public boolean isAdmin() {
        String role = getUserRole();
        return "admin".equalsIgnoreCase(role) || "super admin".equalsIgnoreCase(role);
    }

    // NEW: Check if user is super admin
    public boolean isSuperAdmin() {
        String role = getUserRole();
        return "super admin".equalsIgnoreCase(role);
    }

    // NEW: Check if user can add drivers
    public boolean canAddDriver() {
        return isAdmin(); // Both admin and super admin can add drivers
    }

    // NEW: Check if user can add users
    public boolean canAddUser() {
        return isSuperAdmin(); // Only super admin can add users
    }

    // Get bearer token
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
                .remove(KEY_USER_ROLE)
                .apply();
    }

    // Get authorization header value
    public String getAuthorizationHeader() {
        return getBearerToken();
    }
}
