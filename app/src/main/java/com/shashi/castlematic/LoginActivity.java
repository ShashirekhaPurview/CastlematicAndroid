package com.shashi.castlematic;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shashi.castlematic.core.config.AppConfig;
import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.core.network.AuthRepository;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.AuthResponse;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CastlematicLogin";
    private TextInputLayout usernameLayout, passwordLayout;
    private TextInputEditText usernameInput, passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize managers and repositories
        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();
        authManager = AuthManager.getInstance(this);

        // Initialize API client
        ApiClient.initialize(this);

        // Debug: Log configuration
        Log.d(TAG, "Auth Base URL: " + AppConfig.AUTH_BASE_URL);
        Log.d(TAG, "Client ID: " + AppConfig.CLIENT_ID);

        // If already logged in and has valid API token, go to main activity
        if (sessionManager.isLoggedIn() && authManager.hasValidToken()) {
            navigateToMainActivity();
            return;
        }

        initializeViews();
        setupClickListeners();

        // Pre-fill test credentials for development
        usernameInput.setText(AppConfig.TEST_USERNAME);
        passwordInput.setText(AppConfig.TEST_PASSWORD);
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.password_layout);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());

        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                performLogin();
                return true;
            }
            return false;
        });
    }

    private void performLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        Log.d(TAG, "Login attempt - Username: '" + username + "', Password length: " + password.length());

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            usernameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        // Clear errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        // Show loading
        showLoading(true);

        Log.d(TAG, "Attempting login with username: " + username);

        // Try API login first
        authRepository.login(username, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                runOnUiThread(() -> {
                    showLoading(false);

                    Log.d(TAG, "API login successful. Token type: " + response.tokenType + ", Role: " + response.role);

                    String role = response.role;
                    if (role == null || role.trim().isEmpty()) {
                        role = "admin";
                        Log.d(TAG, "Role was null, defaulting to: " + role);
                    }

                    // Check if user role is blocked
                    if ("user".equalsIgnoreCase(role.trim())) {
                        Toast.makeText(LoginActivity.this,
                                "❌ User role cannot access mobile app.\nPlease use web dashboard.",
                                Toast.LENGTH_LONG).show();
                        authManager.clearToken();
                        passwordInput.setText("");
                        return;
                    }

                    // Save token
                    authManager.saveToken(response.accessToken, response.tokenType, role);
                    sessionManager.createLoginSession(username);

                    Toast.makeText(LoginActivity.this, "✅ Login Successful", Toast.LENGTH_SHORT).show();

                    // NEW: Navigate based on role
                    navigateBasedOnRole(role);
                });
            }

            private void navigateBasedOnRole(String role) {
                if ("driver".equalsIgnoreCase(role.trim())) {
                    // Driver role - go directly to inspection page
                    Intent intent = new Intent(LoginActivity.this,
                            com.shashi.castlematic.features.driver_inspection.DriverInspectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Admin/Super Admin - go to main dashboard
                    navigateToMainActivity();
                }
            }


            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);

                    Log.e(TAG, "API login failed: " + error);

                    // Check if it's a network/server issue - try fallback login
                    if (error.contains("offline") || error.contains("ERR_NGROK") || error.contains("404")) {
                        Toast.makeText(LoginActivity.this,
                                "🌐 Server offline. Using offline mode...", Toast.LENGTH_LONG).show();
                        performFallbackLogin(username, password);
                    } else {
                        // Authentication error
                        passwordInput.setText("");
                        passwordInput.requestFocus();

                        if (error.contains("401") || error.contains("Unauthorized")) {
                            Toast.makeText(LoginActivity.this, "❌ Invalid credentials", Toast.LENGTH_LONG).show();
                            passwordLayout.setError("Invalid credentials");
                        } else {
                            Toast.makeText(LoginActivity.this, "❌ Login failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void performFallbackLogin(String username, String password) {
        // Fallback to hardcoded credentials when server is offline
        if (AppConfig.TEST_USERNAME.equals(username) && AppConfig.TEST_PASSWORD.equals(password)) {
            Log.d(TAG, "Fallback login successful");

            // Create mock token for offline use with super admin role
            authManager.saveToken("offline_token_" + System.currentTimeMillis(), "Bearer", "super admin");

            // Create session
            sessionManager.createLoginSession(username);

            Toast.makeText(this, "✅ Offline Login as Super Admin", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(this, "❌ Invalid credentials", Toast.LENGTH_LONG).show();
            passwordLayout.setError("Invalid credentials");
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        loginButton.setText(show ? "Authenticating..." : "SIGN IN");
        usernameInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!sessionManager.isLoggedIn()) {
            authManager.clearToken();
        }
    }
}
