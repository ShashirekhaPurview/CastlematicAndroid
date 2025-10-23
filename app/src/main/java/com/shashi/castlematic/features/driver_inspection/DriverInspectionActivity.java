package com.shashi.castlematic.features.driver_inspection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.shashi.castlematic.LoginActivity;
import com.shashi.castlematic.R;
import com.shashi.castlematic.SessionManager;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.features.driver_inspection.models.InspectionModels;
import com.shashi.castlematic.features.driver_inspection.models.InspectionModels.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DriverInspectionActivity extends AppCompatActivity implements InspectionAdapter.OnItemInteractionListener {

    private static final String TAG = "DriverInspection";

    private SessionManager sessionManager;
    private AuthManager authManager;

    private TextView driverNameText;
    private RadioGroup inspectionTypeGroup;
    private RadioButton startDutyRadio, endDutyRadio;
    private TextInputEditText fullNameInput, licenseNumberInput, phoneNumberInput,
            aadharCardInput, licenseExpiryInput, dateOfJoinInput;
    private TextInputEditText vehicleNumberInput, odometerInput, engineHoursInput, overallRemarksInput;  // Updated

    private RecyclerView recyclerView;
    private Button submitButton;

    private InspectionAdapter adapter;
    private List<InspectionItem> inspectionItems;

    private int currentPhotoPosition = -1;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_inspection);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        authManager = AuthManager.getInstance(this);

        // Verify driver role
        if (!isDriver()) {
            Toast.makeText(this, "This page is only for drivers", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        setupCameraLauncher();
        setupRecyclerView();
        setupClickListeners();
    }

    private boolean isDriver() {
        String role = authManager.getUserRole();
        return "driver".equalsIgnoreCase(role);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Vehicle Inspection");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void initializeViews() {
        driverNameText = findViewById(R.id.driver_name_text);
        inspectionTypeGroup = findViewById(R.id.inspection_type_group);
        startDutyRadio = findViewById(R.id.radio_start_duty);
        endDutyRadio = findViewById(R.id.radio_end_duty);
        vehicleNumberInput = findViewById(R.id.vehicle_number);
        odometerInput = findViewById(R.id.odometer_reading);
        overallRemarksInput = findViewById(R.id.overall_remarks);
        recyclerView = findViewById(R.id.inspection_items_recycler);
        submitButton = findViewById(R.id.submit_inspection_btn);

        // Set driver name
        String username = sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME);
        driverNameText.setText("Driver: " + (username != null ? username : "Unknown"));
    }

    private void setupCameraLauncher() {
        // Camera permission
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Camera permission required for evidence photos", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Camera capture
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap photo = (Bitmap) extras.get("data");

                        if (photo != null && currentPhotoPosition >= 0) {
                            // Save photo and update adapter
                            String photoPath = savePhotoToInternalStorage(photo);
                            adapter.updateItemPhoto(currentPhotoPosition, photoPath);
                            Toast.makeText(this, "✅ Photo captured", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupRecyclerView() {
        inspectionItems = InspectionModels.getDefaultInspectionItems();
        adapter = new InspectionAdapter(inspectionItems, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        submitButton.setOnClickListener(v -> submitInspection());
    }

    @Override
    public void onCameraClick(InspectionItem item, int position) {
        currentPhotoPosition = position;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    @Override
    public void onCheckChanged(InspectionItem item, boolean isChecked) {
        Log.d(TAG, "Item checked: " + item.title + " = " + isChecked);
    }

    @Override
    public void onRemarksChanged(InspectionItem item, String remarks) {
        Log.d(TAG, "Remarks for " + item.title + ": " + remarks);
    }

    private void submitInspection() {
        // Validate inputs
        String vehicleNumber = vehicleNumberInput.getText().toString().trim();
        String odometerStr = odometerInput.getText().toString().trim();
        String engineHoursStr = engineHoursInput.getText().toString().trim();  // NEW

        if (TextUtils.isEmpty(vehicleNumber)) {
            vehicleNumberInput.setError("Vehicle number is required");
            vehicleNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(odometerStr)) {
            odometerInput.setError("Odometer reading is required");
            odometerInput.requestFocus();
            return;
        }

        // NEW: Validate engine hours
        if (TextUtils.isEmpty(engineHoursStr)) {
            engineHoursInput.setError("Engine hours is required");
            engineHoursInput.requestFocus();
            return;
        }

        int odometerReading;
        double engineHours;

        try {
            odometerReading = Integer.parseInt(odometerStr);
        } catch (NumberFormatException e) {
            odometerInput.setError("Invalid number");
            odometerInput.requestFocus();
            return;
        }

        // NEW: Parse engine hours
        try {
            engineHours = Double.parseDouble(engineHoursStr);
        } catch (NumberFormatException e) {
            engineHoursInput.setError("Invalid number");
            engineHoursInput.requestFocus();
            return;
        }

        // Check if all items are checked
        int checkedCount = 0;
        for (InspectionItem item : inspectionItems) {
            if (item.isChecked) checkedCount++;
        }

        if (checkedCount == 0) {
            Toast.makeText(this, "⚠️ Please check at least one inspection item", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create inspection record
        InspectionRecord record = new InspectionRecord();
        record.driverId = sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME);
        record.driverName = record.driverId;
        record.vehicleNumber = vehicleNumber;
        record.inspectionType = startDutyRadio.isChecked() ? "start" : "end";
        record.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        record.odometerReading = odometerReading;
        record.engineHours = engineHours;  // NEW
        record.items = inspectionItems;
        record.overallRemarks = overallRemarksInput.getText().toString().trim();

        // Show confirmation dialog
        showSubmitConfirmation(record, checkedCount);
    }


    private void showSubmitConfirmation(InspectionRecord record, int checkedCount) {
        String type = record.inspectionType.equals("start") ? "Start of Duty" : "End of Duty";

        new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setTitle("Submit Inspection?")
                .setMessage(String.format("Vehicle: %s\n%s Inspection\nOdometer: %d km\nEngine Hours: %.1f hrs\nItems Checked: %d/%d\n\nSubmit this inspection?",
                        record.vehicleNumber, type, record.odometerReading, record.engineHours, checkedCount, inspectionItems.size()))
                .setPositiveButton("Submit", (dialog, which) -> {
                    Toast.makeText(this, "✅ Inspection submitted successfully!", Toast.LENGTH_LONG).show();
                    showCompletionDialog(record.inspectionType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCompletionDialog(String inspectionType) {
        if ("end".equals(inspectionType)) {
            new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)  // Added theme
                    .setTitle("✅ Inspection Complete")
                    .setMessage("End of duty inspection completed.\nWould you like to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> logout())
                    .setNegativeButton("Stay", (dialog, which) -> clearForm())
                    .setCancelable(false)
                    .show();
        } else {
            clearForm();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)  // Added theme
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void clearForm() {
        vehicleNumberInput.setText("");
        odometerInput.setText("");
        engineHoursInput.setText("");  // NEW
        overallRemarksInput.setText("");
        startDutyRadio.setChecked(true);

        // Reset checklist
        inspectionItems = InspectionModels.getDefaultInspectionItems();
        adapter = new InspectionAdapter(inspectionItems, this);
        recyclerView.setAdapter(adapter);
    }


    private String savePhotoToInternalStorage(Bitmap bitmap) {
        // TODO: Implement actual photo saving to internal storage
        // For now, just return a placeholder
        return "photo_" + System.currentTimeMillis() + ".jpg";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        authManager.clearToken();
        sessionManager.logoutUser();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionManager.updateLastActivity();
    }

    @Override
    public void onBackPressed() {
        // Prevent back navigation for drivers
        Toast.makeText(this, "Please use logout from menu", Toast.LENGTH_SHORT).show();
    }
}
