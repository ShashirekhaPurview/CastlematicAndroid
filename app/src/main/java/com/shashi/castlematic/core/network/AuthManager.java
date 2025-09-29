package com.shashi.castlematic.core.network;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "castlematic_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    private SharedPreferences prefs;
    private static AuthManager instance;

    private AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(String accessToken, String tokenType) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_TOKEN_TYPE, tokenType)
                .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000)) // 24 hours
                .apply();
    }

    public String getAccessToken() {
        // Check if token is expired
        long expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        if (System.currentTimeMillis() > expiry) {
            clearToken();
            return null;
        }
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getTokenType() {
        return prefs.getString(KEY_TOKEN_TYPE, "Bearer");
    }

    public String getBearerToken() {
        String token = getAccessToken();
        if (token != null) {
            return getTokenType() + " " + token;
        }
        return null;
    }

    public boolean hasValidToken() {
        return getAccessToken() != null;
    }

    public void clearToken() {
        prefs.edit().clear().apply();
    }
}
