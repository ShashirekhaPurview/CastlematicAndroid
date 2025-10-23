package com.shashi.castlematic.features.user_management;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.shashi.castlematic.LoginActivity;
import com.shashi.castlematic.R;
import com.shashi.castlematic.SessionManager;
import com.shashi.castlematic.core.network.AuthManager;

public class UserManagementActivity extends AppCompatActivity {

    private static final String TAG = "UserManagementActivity";

    private SessionManager sessionManager;
    private AuthManager authManager;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserManagementPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        authManager = AuthManager.getInstance(this);

        setupAuthentication();
        setupToolbar();
        initializeViews();
        setupViewPager();
    }

    private void setupAuthentication() {
        if (!authManager.hasValidToken()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (!authManager.canAddDriver()) {
            Toast.makeText(this, "❌ You don't have permission to access User Management",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
    }

    private void setupViewPager() {
        boolean canAddUser = authManager.canAddUser();

        pagerAdapter = new UserManagementPagerAdapter(this, canAddUser);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Add Driver");
                tab.setIcon(R.drawable.ic_driver);
            } else {
                tab.setText("Add User");
                tab.setIcon(R.drawable.ic_user);
            }
        }).attach();

        if (!canAddUser) {
            Toast.makeText(this,
                    "ℹ️ Admin users can only add drivers.\nContact Super Admin to add new users.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionManager.updateLastActivity();
    }
}
