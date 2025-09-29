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

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "CastlematicSplash";
    private static final int SPLASH_TIME_OUT = 2000;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize session manager
        sessionManager = new SessionManager(this);

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

            // Navigate based on login status
            new Handler().postDelayed(() -> {
                try {
                    Intent intent;
                    if (sessionManager.isLoggedIn()) {
                        Log.d(TAG, "User already logged in, navigating to MainActivity");
                        intent = new Intent(SplashActivity.this, MainActivity.class);
                    } else {
                        Log.d(TAG, "User not logged in, navigating to LoginActivity");
                        intent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating: " + e.getMessage());
                }
            }, SPLASH_TIME_OUT);

        } catch (Exception e) {
            Log.e(TAG, "Error in SplashActivity onCreate: " + e.getMessage());
        }
    }
}
