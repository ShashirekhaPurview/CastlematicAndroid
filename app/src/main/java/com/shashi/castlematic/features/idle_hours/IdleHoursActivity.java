package com.shashi.castlematic.features.idle_hours;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.shashi.castlematic.R;
import com.shashi.castlematic.SessionManager;
import com.shashi.castlematic.LoginActivity;
import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.features.idle_hours.repository.IdleHoursRepository;
import com.shashi.castlematic.features.idle_hours.models.IdleModels.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class IdleHoursActivity extends AppCompatActivity {

    private static final String TAG = "IdleHoursActivity";
    private static final int ITEMS_PER_PAGE = 10;

    private SessionManager sessionManager;
    private IdleHoursRepository repository;

    // Date selection (single date)
    private LinearLayout dateSelectorContainer;
    private TextView selectedDateText;
    private Button loadDataBtn;
    private String selectedDate;

    // Summary
    private TextView totalVehiclesCount, totalIdleHours, showingCountText;

    // Pagination
    private LinearLayout paginationContainer;
    private Button prevPageBtn, nextPageBtn;
    private TextView pageInfoText;
    private int currentPage = 1;

    // Vehicle list
    private LinearLayout vehiclesContainer;
    private List<VehicleIdle> allVehicles = new ArrayList<>();
    private List<VehicleIdle> currentPageVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idle_hours);

        ApiClient.initialize(this);
        setupAuthentication();

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        repository = new IdleHoursRepository();

        // Set default date to yesterday
        setYesterday();

        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadData();
    }

    private void setupAuthentication() {
        AuthManager authManager = AuthManager.getInstance(this);

        if (!authManager.hasValidToken()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vehicle Idle Hours");
        }
    }

    private void initializeViews() {
        // Date selection
        dateSelectorContainer = findViewById(R.id.date_selector_container);
        selectedDateText = findViewById(R.id.selected_date_text);
        loadDataBtn = findViewById(R.id.load_data_btn);

        // Summary
        totalVehiclesCount = findViewById(R.id.total_vehicles_count);
        totalIdleHours = findViewById(R.id.total_idle_hours);
        showingCountText = findViewById(R.id.showing_count_text);

        // Pagination
        paginationContainer = findViewById(R.id.pagination_container);
        prevPageBtn = findViewById(R.id.prev_page_btn);
        nextPageBtn = findViewById(R.id.next_page_btn);
        pageInfoText = findViewById(R.id.page_info_text);

        // Vehicle list
        vehiclesContainer = findViewById(R.id.vehicles_container);

        updateDateDisplay();
    }

    private void setupClickListeners() {
        // Date picker
        dateSelectorContainer.setOnClickListener(v -> showDatePicker());

        // Load data
        loadDataBtn.setOnClickListener(v -> {
            currentPage = 1;
            loadData();
        });

        // Pagination
        prevPageBtn.setOnClickListener(v -> previousPage());
        nextPageBtn.setOnClickListener(v -> nextPage());
    }

    private void setYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // Yesterday

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(cal.getTime());
    }

    private void updateDateDisplay() {
        selectedDateText.setText(selectedDate);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Set max date to yesterday (no today or future dates)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long maxDate = calendar.getTimeInMillis();

        // Parse current selected date
        String[] dateParts = selectedDate.split("-");
        calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2]));

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Block today and future dates
        datePickerDialog.getDatePicker().setMaxDate(maxDate);
        datePickerDialog.show();
    }

    private void loadData() {
        Toast.makeText(this, "🔄 Loading idle hours for " + selectedDate + "...", Toast.LENGTH_SHORT).show();

        repository.getIdlingHours(selectedDate, selectedDate, "Both",
                new IdleHoursRepository.DataCallback<VehicleIdleResponse>() {
                    @Override
                    public void onSuccess(VehicleIdleResponse response) {
                        runOnUiThread(() -> {
                            allVehicles = response.vehicles != null ? response.vehicles : new ArrayList<>();

                            // Initialize remaining hours for each vehicle
                            for (VehicleIdle vehicle : allVehicles) {
                                vehicle.remainingHours = vehicle.getIdleHoursDecimal();
                            }

                            currentPage = 1;
                            updateDisplay();
                            Toast.makeText(IdleHoursActivity.this,
                                    "✅ Loaded " + allVehicles.size() + " vehicles", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(IdleHoursActivity.this,
                                    "❌ Error: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error loading data: " + error);
                        });
                    }
                });
    }

    private void updateDisplay() {
        updateSummary();
        updatePagination();
        displayCurrentPage();
    }

    private void updateSummary() {
        totalVehiclesCount.setText(String.valueOf(allVehicles.size()));

        double totalHours = 0.0;
        for (VehicleIdle vehicle : allVehicles) {
            totalHours += vehicle.getIdleHoursDecimal();
        }

        int hours = (int) totalHours;
        int minutes = (int) ((totalHours - hours) * 60);
        totalIdleHours.setText(String.format(Locale.getDefault(), "%d:%02d", hours, minutes));
    }

    private void updatePagination() {
        int totalPages = (int) Math.ceil((double) allVehicles.size() / ITEMS_PER_PAGE);

        if (totalPages > 1) {
            paginationContainer.setVisibility(android.view.View.VISIBLE);
            pageInfoText.setText("Page " + currentPage + " of " + totalPages);

            // Enable/disable buttons
            prevPageBtn.setEnabled(currentPage > 1);
            nextPageBtn.setEnabled(currentPage < totalPages);

            // Style buttons based on state
            prevPageBtn.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
            nextPageBtn.setAlpha(currentPage < totalPages ? 1.0f : 0.5f);
        } else {
            paginationContainer.setVisibility(android.view.View.GONE);
        }

        // Update showing count
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allVehicles.size());
        showingCountText.setText("Showing " + (startIndex + 1) + "-" + endIndex + " of " + allVehicles.size());
    }

    private void displayCurrentPage() {
        vehiclesContainer.removeAllViews();

        if (allVehicles.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("📭 No vehicles with idle hours found for " + selectedDate);
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.setGravity(android.view.Gravity.CENTER);
            vehiclesContainer.addView(emptyView);
            return;
        }

        // Calculate pagination
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allVehicles.size());

        currentPageVehicles.clear();
        for (int i = startIndex; i < endIndex; i++) {
            VehicleIdle vehicle = allVehicles.get(i);
            currentPageVehicles.add(vehicle);
            displayVehicleCard(vehicle);
        }
    }

    private void displayVehicleCard(VehicleIdle vehicle) {
        android.view.View cardView = getLayoutInflater().inflate(R.layout.item_vehicle_idle, null);

        // Set margins for spacing
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(params);

        TextView vehicleName = cardView.findViewById(R.id.vehicle_name);
        TextView idleHours = cardView.findViewById(R.id.idle_hours);
        TextView category = cardView.findViewById(R.id.category);
        TextView project = cardView.findViewById(R.id.project);
        TextView ownedHired = cardView.findViewById(R.id.owned_hired);
        Button assignReasonBtn = cardView.findViewById(R.id.assign_reason_btn);

        vehicleName.setText(vehicle.getCleanVehicleName());
        idleHours.setText(vehicle.getFormattedIdleHours());
        category.setText(vehicle.vehicleCategory);
        project.setText(vehicle.project);
        ownedHired.setText(vehicle.ownedOrHired);

        // Color code based on owned/hired
        if ("Owned".equalsIgnoreCase(vehicle.ownedOrHired)) {
            ownedHired.setBackgroundColor(getResources().getColor(R.color.success_green));
        } else {
            ownedHired.setBackgroundColor(getResources().getColor(R.color.info_blue));
        }

        assignReasonBtn.setOnClickListener(v -> showReasonAssignmentDialog(vehicle));

        vehiclesContainer.addView(cardView);
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateDisplay();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) allVehicles.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            updateDisplay();
        }
    }

    // REASON ASSIGNMENT DIALOG
    // UPDATED: REASON ASSIGNMENT DIALOG WITH HOUR/MINUTE PICKERS
    // CORRECTED: REASON ASSIGNMENT DIALOG WITH IMAGEBUTTONS
    private void showReasonAssignmentDialog(VehicleIdle vehicle) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_assign_reason, null);

        // Get dialog views
        TextView vehicleNameText = dialogView.findViewById(R.id.dialog_vehicle_name);
        TextView totalIdleHoursText = dialogView.findViewById(R.id.dialog_total_idle_hours);
        Spinner reasonSpinner = dialogView.findViewById(R.id.reason_spinner);

        // FIXED: Use ImageButton instead of Button
        android.widget.ImageButton decreaseHoursBtn = dialogView.findViewById(R.id.decrease_hours_btn);
        android.widget.ImageButton increaseHoursBtn = dialogView.findViewById(R.id.increase_hours_btn);
        TextView hoursValue = dialogView.findViewById(R.id.hours_value);
        android.widget.ImageButton decreaseMinutesBtn = dialogView.findViewById(R.id.decrease_minutes_btn);
        android.widget.ImageButton increaseMinutesBtn = dialogView.findViewById(R.id.increase_minutes_btn);
        TextView minutesValue = dialogView.findViewById(R.id.minutes_value);

        Button assignButton = dialogView.findViewById(R.id.assign_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Set vehicle info
        vehicleNameText.setText(vehicle.getCleanVehicleName());
        totalIdleHoursText.setText("Idle Hours: " + vehicle.getFormattedIdleHours());

        // Calculate max hours and minutes from idle hours
        String[] idleParts = vehicle.idlingHours.split(":");
        final int maxHours = Integer.parseInt(idleParts[0]);
        final int maxMinutes = idleParts.length > 1 ? Integer.parseInt(idleParts[1]) : 0;

        // Current selected values
        final int[] currentHours = {0};
        final int[] currentMinutes = {0};

        // Setup reason spinner (only 4 options)
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                IdleReason.getAllReasons()
        );
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reasonSpinner.setAdapter(reasonAdapter);

        // Helper method to update display
        Runnable updateDisplay = () -> {
            hoursValue.setText(String.valueOf(currentHours[0]));
            minutesValue.setText(String.format(Locale.getDefault(), "%02d", currentMinutes[0]));

            // Update button states
            decreaseHoursBtn.setEnabled(currentHours[0] > 0);
            increaseHoursBtn.setEnabled(currentHours[0] < maxHours);

            decreaseMinutesBtn.setEnabled(currentMinutes[0] > 0);

            // Minutes max depends on hours
            if (currentHours[0] == maxHours) {
                increaseMinutesBtn.setEnabled(currentMinutes[0] < maxMinutes);
            } else {
                increaseMinutesBtn.setEnabled(currentMinutes[0] < 59);
            }

            // Style disabled buttons (alpha)
            decreaseHoursBtn.setAlpha(currentHours[0] > 0 ? 1.0f : 0.3f);
            increaseHoursBtn.setAlpha(currentHours[0] < maxHours ? 1.0f : 0.3f);
            decreaseMinutesBtn.setAlpha(currentMinutes[0] > 0 ? 1.0f : 0.3f);

            if (currentHours[0] == maxHours) {
                increaseMinutesBtn.setAlpha(currentMinutes[0] < maxMinutes ? 1.0f : 0.3f);
            } else {
                increaseMinutesBtn.setAlpha(currentMinutes[0] < 59 ? 1.0f : 0.3f);
            }
        };

        // Decrease Hours Button
        decreaseHoursBtn.setOnClickListener(v -> {
            if (currentHours[0] > 0) {
                currentHours[0]--;
                updateDisplay.run();
            }
        });

        // Increase Hours Button
        increaseHoursBtn.setOnClickListener(v -> {
            if (currentHours[0] < maxHours) {
                currentHours[0]++;
                // If we hit max hours, reset minutes if they exceed max
                if (currentHours[0] == maxHours && currentMinutes[0] > maxMinutes) {
                    currentMinutes[0] = maxMinutes;
                }
                updateDisplay.run();
            }
        });

        // Decrease Minutes Button
        decreaseMinutesBtn.setOnClickListener(v -> {
            if (currentMinutes[0] > 0) {
                currentMinutes[0]--;
                updateDisplay.run();
            }
        });

        // Increase Minutes Button
        increaseMinutesBtn.setOnClickListener(v -> {
            int minuteLimit = (currentHours[0] == maxHours) ? maxMinutes : 59;
            if (currentMinutes[0] < minuteLimit) {
                currentMinutes[0]++;
                updateDisplay.run();
            }
        });

        // Initialize display
        updateDisplay.run();

        // Create dialog
        android.app.AlertDialog dialog = builder.setView(dialogView).create();

        // Assign button click
        assignButton.setOnClickListener(v -> {
            // Validate that at least some time is selected
            if (currentHours[0] == 0 && currentMinutes[0] == 0) {
                Toast.makeText(this, "⚠️ Please select hours/minutes to assign", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate total hours as decimal
            double assignedHours = currentHours[0] + (currentMinutes[0] / 60.0);
            String selectedReason = reasonSpinner.getSelectedItem().toString();

            // Show confirmation
            confirmAssignment(vehicle, assignedHours, currentHours[0], currentMinutes[0], selectedReason, dialog);
        });

        // Cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // UPDATED: Confirmation with hours and minutes display
    // FIXED: Confirmation dialog with proper text color
    private void confirmAssignment(VehicleIdle vehicle, double hours, int displayHours, int displayMinutes,
                                   String reason, android.app.AlertDialog parentDialog) {

        // Create custom view for confirmation dialog
        LinearLayout dialogView = new LinearLayout(this);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(24, 20, 24, 20);
        dialogView.setBackgroundColor(getResources().getColor(android.R.color.white));

        // Warning icon
        TextView iconView = new TextView(this);
        iconView.setText("⚠️");
        iconView.setTextSize(32);
        iconView.setGravity(android.view.Gravity.CENTER);
        iconView.setPadding(0, 0, 0, 16);
        dialogView.addView(iconView);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Confirm Assignment");
        titleView.setTextSize(18);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(android.view.Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);
        dialogView.addView(titleView);

        // Message
        TextView messageView = new TextView(this);
        String message = String.format(Locale.getDefault(),
                "Assign %d:%02d hours to '%s' for vehicle %s?",
                displayHours, displayMinutes, reason, vehicle.getCleanVehicleName());
        messageView.setText(message);
        messageView.setTextSize(14);
        messageView.setTextColor(getResources().getColor(R.color.text_secondary));
        messageView.setGravity(android.view.Gravity.CENTER);
        messageView.setPadding(0, 0, 0, 24);
        dialogView.addView(messageView);

        // Create dialog
        androidx.appcompat.app.AlertDialog confirmDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("✅ Confirm", (dialog, which) -> {
                    assignReason(vehicle, hours, reason, parentDialog);
                })
                .setNegativeButton("❌ Cancel", null)
                .create();

        confirmDialog.show();

        // Style the buttons
        confirmDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.success_green));
        confirmDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.error_red));
    }


    private void assignReason(VehicleIdle vehicle, double hours, String reason, android.app.AlertDialog parentDialog) {
        // Format hours as string
        String hoursStr = String.format(Locale.getDefault(), "%.2f", hours);

        AssignReasonRequest request = new AssignReasonRequest(
                vehicle,
                selectedDate,
                hoursStr,
                reason
        );

        Toast.makeText(this, "⏳ Assigning reason...", Toast.LENGTH_SHORT).show();

        repository.assignIdleReason(request, new IdleHoursRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    parentDialog.dismiss();
                    Toast.makeText(IdleHoursActivity.this,
                            "✅ Reason assigned successfully", Toast.LENGTH_SHORT).show();

                    // Reload data to get updated idle hours
                    loadData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(IdleHoursActivity.this,
                            "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error assigning reason: " + error);
                });
            }
        });
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
