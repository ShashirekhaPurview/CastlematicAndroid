package com.shashi.castlematic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.core.network.AuthManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SessionManager sessionManager;
    private ActionBarDrawerToggle toggle;

    private CardView fuelTheftCard, fuelIssueCard, consumptionCard, idleReasonCard, utilAvailCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize API client
        ApiClient.initialize(this);

        // Initialize session manager
        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        // Set up authentication and check role
        AuthManager authManager = AuthManager.getInstance(this);

        // NEW: Prevent drivers from accessing admin dashboard
        if ("driver".equalsIgnoreCase(authManager.getUserRole())) {
            Log.d("MainActivity", "Driver detected - redirecting to inspection page");
            Intent intent = new Intent(this,
                    com.shashi.castlematic.features.driver_inspection.DriverInspectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Continue with normal setup for admin/super admin
        setupAuthentication();
        setupToolbar();
        setupNavigationDrawer();
        updateNavigationHeader();
        initializeViews();
        setupClickListeners();
    }


    private void setupAuthentication() {
        AuthManager authManager = AuthManager.getInstance(this);

        // Check if we have a valid token from login
        if (!authManager.hasValidToken()) {
            // If no valid token, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        // REMOVED: Don't show toast here - too many toasts cause the issue
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set toolbar title and colors
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Castlematic Dashboard");
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        drawerLayout = findViewById(R.id.drawer_layout);

        // Create toggle with white hamburger icon
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                sessionManager.updateLastActivity();
            }
        };

        // Set the hamburger icon color to white
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigationDrawer() {
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_username);
        TextView navEmail = headerView.findViewById(R.id.nav_email);
        TextView navLoginTime = headerView.findViewById(R.id.nav_login_time);

        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        String username = userDetails.get(SessionManager.KEY_USERNAME);
        String loginTime = userDetails.get(SessionManager.KEY_LOGIN_TIME);

        navUsername.setText(username != null ? username : "MEIL User");
        navEmail.setText("fleetmanager@meil.in");

        if (loginTime != null && !loginTime.equals("0")) {
            long timestamp = Long.parseLong(loginTime);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            navLoginTime.setText("Last login: " + sdf.format(new Date(timestamp)));
        } else {
            navLoginTime.setText("Welcome to Fleet Management!");
        }
    }

    private void initializeViews() {
        fuelTheftCard = findViewById(R.id.fuel_theft_card);
        fuelIssueCard = findViewById(R.id.fuel_issue_card);
        consumptionCard = findViewById(R.id.consumption_card);
        idleReasonCard = findViewById(R.id.idle_reason_card);
        utilAvailCard = findViewById(R.id.util_avail_card);
    }

    private void setupClickListeners() {
        fuelTheftCard.setOnClickListener(v -> {
            sessionManager.updateLastActivity();
            // REMOVED: Toast to reduce queue
            Intent intent = new Intent(this, com.shashi.castlematic.features.fuel_theft.FuelTheftActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        fuelIssueCard.setOnClickListener(v -> {
            sessionManager.updateLastActivity();
            Toast.makeText(this, "Fuel Issue - Coming Soon", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to FuelIssueActivity
        });

        consumptionCard.setOnClickListener(v -> {
            sessionManager.updateLastActivity();
            Toast.makeText(this, "Consumption - Coming Soon", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to ConsumptionActivity
        });

        // FIXED: Navigate to IdleHoursActivity
        idleReasonCard.setOnClickListener(v -> {
            sessionManager.updateLastActivity();
            Intent intent = new Intent(this, com.shashi.castlematic.features.idle_hours.IdleHoursActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        utilAvailCard.setOnClickListener(v -> {
            sessionManager.updateLastActivity();
            Intent intent = new Intent(this, com.shashi.castlematic.features.utilization.UtilizationActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on dashboard, just close drawer
        } else if (id == R.id.nav_fuel_theft) {
            sessionManager.updateLastActivity();
            Intent intent = new Intent(this, com.shashi.castlematic.features.fuel_theft.FuelTheftActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_idle_hours) {
            sessionManager.updateLastActivity();
            Intent intent = new Intent(this, com.shashi.castlematic.features.idle_hours.IdleHoursActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_utilization) {
            sessionManager.updateLastActivity();
            Intent intent = new Intent(this, com.shashi.castlematic.features.utilization.UtilizationActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_user_management) {
            // NEW: User Management with role-based access
            sessionManager.updateLastActivity();
            AuthManager authManager = AuthManager.getInstance(this);

            if (authManager.canAddDriver()) {
                Intent intent = new Intent(this, com.shashi.castlematic.features.user_management.UserManagementActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "❌ You don't have permission to access User Management",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            showLogoutConfirmation();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from Castlematic?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Clear API token on logout
                    AuthManager.getInstance(this).clearToken();
                    sessionManager.logoutUser();
                    Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("About Castlematic")
                .setMessage("Castlematic v1.0\n\nFleet Management & Fuel Analytics\nfor Heavy Equipment & Construction Vehicles\n\nTrack | Control | Optimize\n\nDeveloped for MEIL Group\n© 2025 All rights reserved\n\nAPI Status: Connected ✓")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionManager.updateLastActivity();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }
}
