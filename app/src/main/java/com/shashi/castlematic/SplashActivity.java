package com.shashi.castlematic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.shashi.castlematic.core.network.AuthManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "CastlematicSplash";
    private static final int SPLASH_TIME_OUT = 2000;

    private SessionManager sessionManager;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize managers
        sessionManager = new SessionManager(this);
        authManager = AuthManager.getInstance(this);

        Log.d(TAG, "SplashActivity onCreate started");

        try {
            // Initialize views and animations
            ImageView logoImage = findViewById(R.id.splash_logo);
            TextView appName = findViewById(R.id.app_name);
            TextView tagline = findViewById(R.id.tagline);

            // Load animations
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            logoImage.startAnimation(fadeIn);
            appName.startAnimation(fadeIn);
            tagline.startAnimation(fadeIn);

            // Navigate based on login status and role
            new Handler().postDelayed(() -> {
                try {
                    navigateBasedOnLoginStatus();
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating: " + e.getMessage());
                }
            }, SPLASH_TIME_OUT);

        } catch (Exception e) {
            Log.e(TAG, "Error in SplashActivity onCreate: " + e.getMessage());
        }
    }

    private void navigateBasedOnLoginStatus() {
        Intent intent;

        if (sessionManager.isLoggedIn() && authManager.hasValidToken()) {
            // User is logged in - check role
            String role = authManager.getUserRole();
            Log.d(TAG, "User logged in with role: " + role);

            if ("driver".equalsIgnoreCase(role)) {
                // Driver - go to inspection page
                Log.d(TAG, "Navigating to DriverInspectionActivity");
                intent = new Intent(this,
                        com.shashi.castlematic.features.driver_inspection.DriverInspectionActivity.class);
            } else {
                // Admin/Super Admin - go to main dashboard
                Log.d(TAG, "Navigating to MainActivity");
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // User not logged in
            Log.d(TAG, "User not logged in, navigating to LoginActivity");
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
