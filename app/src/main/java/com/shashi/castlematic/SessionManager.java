package com.shashi.castlematic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    // SharedPreferences file name
    private static final String PREF_NAME = "CastlematicSession";
    private static final String LOGIN_STATUS = "isLoggedIn";

    // User session keys
    public static final String KEY_USERNAME = "username";
    public static final String KEY_LOGIN_TIME = "loginTime";
    public static final String KEY_LAST_ACTIVITY = "lastActivity";

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String username) {
        editor.putBoolean(LOGIN_STATUS, true);
        editor.putString(KEY_USERNAME, username);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Check login status - if false, redirect to login
     */
    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Get user details from session
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_USERNAME, sharedPreferences.getString(KEY_USERNAME, null));
        user.put(KEY_LOGIN_TIME, String.valueOf(sharedPreferences.getLong(KEY_LOGIN_TIME, 0)));
        return user;
    }

    /**
     * Update last activity time
     */
    public void updateLastActivity() {
        editor.putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Clear session and logout
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Quick check for login status
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(LOGIN_STATUS, false);
    }

    /**
     * Get username
     */
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "User");
    }
}
