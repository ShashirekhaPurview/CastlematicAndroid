package com.shashi.castlematic.features.fuel_theft;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.shashi.castlematic.R;
import com.shashi.castlematic.SessionManager;
import com.shashi.castlematic.LoginActivity;
import com.shashi.castlematic.core.utils.DateHelper;
import com.shashi.castlematic.core.utils.DateUtils;
import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.features.fuel_theft.repository.FuelTheftRepository;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.shashi.castlematic.core.utils.DateUtils;

public class FuelTheftActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FuelTheftRepository repository;

    // Date selection with dropdown
    private Spinner dateRangeSpinner;
    private LinearLayout customDateContainer;
    private LinearLayout fromDateContainer, toDateContainer;
    private TextView fromDateText, toDateText, selectedRangeText;
    private Button refreshBtn;
    private String selectedFromDate;
    private String selectedToDate;

    // Date range options
    private String[] dateRangeOptions = {
            "Yesterday",
            "Past 7 Days",
            "Past 30 Days",
            "Past 90 Days",
            "Custom Range"
    };

    // Summary Cards
    private CardView totalCard, pendingAlertsCard, actionPendingCard, resolvedCard, falseCard;
    private TextView totalCount, pendingAlertsCount, actionPendingCount, resolvedCount, falseCount;
    private TextView totalFuel, pendingAlertsFuel, actionPendingFuel, resolvedFuel, falseFuel;

    // Filter and Pagination
    private LinearLayout filterHeader, paginationHeader;
    private TextView filterTitle, recordCountText, pageInfo;
    private Button clearFilterBtn, prevBtn, nextBtn;
    private LinearLayout dataContainer;

    // Data lists
    private List<TheftAlert> pendingAlertsList = new ArrayList<>();
    private List<TheftLog> actionPendingList = new ArrayList<>();
    private List<TheftLog> resolvedList = new ArrayList<>();
    private List<TheftLog> falseList = new ArrayList<>();

    // Pagination
    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private List<Object> filteredData = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuel_theft);

        ApiClient.initialize(this);
        setupAuthentication();

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        repository = new FuelTheftRepository();

        // Set default date range to yesterday
        setYesterdayRange();

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
            getSupportActionBar().setTitle("Fuel Theft Management");
        }
    }

    private void initializeViews() {
        // Date range selector
        dateRangeSpinner = findViewById(R.id.date_range_spinner);
        customDateContainer = findViewById(R.id.custom_date_container);
        fromDateContainer = findViewById(R.id.from_date_container);
        toDateContainer = findViewById(R.id.to_date_container);
        fromDateText = findViewById(R.id.from_date_text);
        toDateText = findViewById(R.id.to_date_text);
        selectedRangeText = findViewById(R.id.selected_range_text);
        refreshBtn = findViewById(R.id.refresh_btn);

        // Setup date range spinner
        setupDateRangeSpinner();
        updateDateDisplays();

        // Summary cards
        totalCard = findViewById(R.id.total_card);
        pendingAlertsCard = findViewById(R.id.pending_alerts_card);
        actionPendingCard = findViewById(R.id.action_pending_card);
        resolvedCard = findViewById(R.id.resolved_card);
        falseCard = findViewById(R.id.false_card);

        // Count TextViews
        totalCount = findViewById(R.id.total_count);
        pendingAlertsCount = findViewById(R.id.pending_alerts_count);
        actionPendingCount = findViewById(R.id.action_pending_count);
        resolvedCount = findViewById(R.id.resolved_count);
        falseCount = findViewById(R.id.false_count);

        // Fuel TextViews
        totalFuel = findViewById(R.id.total_fuel);
        pendingAlertsFuel = findViewById(R.id.pending_alerts_fuel);
        actionPendingFuel = findViewById(R.id.action_pending_fuel);
        resolvedFuel = findViewById(R.id.resolved_fuel);
        falseFuel = findViewById(R.id.false_fuel);

        // Filter and pagination
        filterHeader = findViewById(R.id.filter_header);
        paginationHeader = findViewById(R.id.pagination_header);
        filterTitle = findViewById(R.id.filter_title);
        recordCountText = findViewById(R.id.record_count_text);
        pageInfo = findViewById(R.id.page_info);
        clearFilterBtn = findViewById(R.id.clear_filter_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn = findViewById(R.id.next_btn);
        dataContainer = findViewById(R.id.data_container);
    }

    private void setupDateRangeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                dateRangeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateRangeSpinner.setAdapter(adapter);

        // Set default to "Yesterday" (index 0)
        dateRangeSpinner.setSelection(0);
    }

    private void setupClickListeners() {
        // Date range spinner listener
        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                onDateRangeSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Custom date pickers (only show when Custom Range is selected)
        fromDateContainer.setOnClickListener(v -> showDatePicker(true));
        toDateContainer.setOnClickListener(v -> showDatePicker(false));

        refreshBtn.setOnClickListener(v -> loadData());

        // Card clicks to filter data
        totalCard.setOnClickListener(v -> filterData("ALL"));
        pendingAlertsCard.setOnClickListener(v -> filterData("PENDING_ALERTS"));
        actionPendingCard.setOnClickListener(v -> filterData("ACTION_PENDING"));
        resolvedCard.setOnClickListener(v -> filterData("RESOLVED"));
        falseCard.setOnClickListener(v -> filterData("FALSE"));

        // Filter and pagination controls
        clearFilterBtn.setOnClickListener(v -> clearFilter());
        prevBtn.setOnClickListener(v -> previousPage());
        nextBtn.setOnClickListener(v -> nextPage());
    }

    private void onDateRangeSelected(int position) {
        switch (position) {
            case 0: // Yesterday
                setYesterdayRange();
                customDateContainer.setVisibility(android.view.View.GONE);
                break;
            case 1: // Past 7 Days
                setPastDaysRange(7);
                customDateContainer.setVisibility(android.view.View.GONE);
                break;
            case 2: // Past 30 Days
                setPastDaysRange(30);
                customDateContainer.setVisibility(android.view.View.GONE);
                break;
            case 3: // Past 90 Days
                setPastDaysRange(90);
                customDateContainer.setVisibility(android.view.View.GONE);
                break;
            case 4: // Custom Range
                customDateContainer.setVisibility(android.view.View.VISIBLE);
                break;
        }

        if (position != 4) { // Auto-load data for preset ranges
            updateDateDisplays();
            loadData();
        } else {
            updateDateDisplays();
        }
    }

    private void setYesterdayRange() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // Yesterday

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedFromDate = sdf.format(cal.getTime());
        selectedToDate = selectedFromDate; // Same date for yesterday
    }

    private void setPastDaysRange(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // To date is yesterday

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedToDate = sdf.format(cal.getTime());

        cal.add(Calendar.DAY_OF_MONTH, -(days - 1)); // From date
        selectedFromDate = sdf.format(cal.getTime());
    }

    private void updateDateDisplays() {
        fromDateText.setText(selectedFromDate);
        toDateText.setText(selectedToDate);

        // Update selected range text
        int selectedPosition = dateRangeSpinner.getSelectedItemPosition();
        String rangeText;

        if (selectedPosition == 4) { // Custom range
            rangeText = "Data for: " + selectedFromDate + " to " + selectedToDate;
        } else {
            rangeText = "Data for: " + dateRangeOptions[selectedPosition];
        }

        selectedRangeText.setText(rangeText);
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();

        // Set max date to yesterday (no future dates allowed)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long maxDate = calendar.getTimeInMillis();

        // Reset calendar for initial date
        calendar = Calendar.getInstance();
        if (isFromDate) {
            // For from date, parse selectedFromDate
            String[] dateParts = selectedFromDate.split("-");
            calendar.set(Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2]));
        } else {
            // For to date, parse selectedToDate
            String[] dateParts = selectedToDate.split("-");
            calendar.set(Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2]));
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    if (isFromDate) {
                        // Validate from date is not after to date
                        if (selectedDate.compareTo(selectedToDate) > 0) {
                            Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedFromDate = selectedDate;
                    } else {
                        // Validate to date is not before from date
                        if (selectedDate.compareTo(selectedFromDate) < 0) {
                            Toast.makeText(this, "To date cannot be before From date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedToDate = selectedDate;
                    }

                    updateDateDisplays();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to yesterday
        datePickerDialog.getDatePicker().setMaxDate(maxDate);

        datePickerDialog.show();
    }

    private void loadData() {
        String ownedOrHired = "both";
        Toast.makeText(this, "🔄 Loading: " + selectedRangeText.getText(), Toast.LENGTH_SHORT).show();

        // Load pending alerts
        repository.getTheftAlerts(selectedFromDate, selectedToDate, ownedOrHired, new FuelTheftRepository.DataCallback<List<TheftAlert>>() {
            @Override
            public void onSuccess(List<TheftAlert> data) {
                pendingAlertsList.clear();
                if (data != null) {
                    for (TheftAlert alert : data) {
                        if (!alert.isFuelTheftConfirmed && alert.isActualTheftAlert) {
                            pendingAlertsList.add(alert);
                        }
                    }
                }

                runOnUiThread(() -> {
                    updateSummaryCards();
                    if (currentFilter.equals("PENDING_ALERTS") || currentFilter.equals("ALL")) {
                        displayCurrentFilter();
                    }
                    Toast.makeText(FuelTheftActivity.this, "📊 Found " + pendingAlertsList.size() + " pending alerts", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FuelTheftActivity.this, "❌ Error loading alerts: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });

        // Load theft logs
        repository.getTheftLogs(selectedFromDate, selectedToDate, ownedOrHired, new FuelTheftRepository.DataCallback<List<TheftLog>>() {
            @Override
            public void onSuccess(List<TheftLog> data) {
                categorizeTheftLogs(data != null ? data : new ArrayList<>());
                runOnUiThread(() -> {
                    updateSummaryCards();
                    displayCurrentFilter();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FuelTheftActivity.this, "❌ Error loading logs: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Rest of your existing methods (categorizeTheftLogs, updateSummaryCards, etc.) remain the same...
    private void categorizeTheftLogs(List<TheftLog> logs) {
        actionPendingList.clear();
        resolvedList.clear();
        falseList.clear();

        for (TheftLog log : logs) {
            if (!log.isActualTheft) {
                falseList.add(log);
            } else if (log.isResolved) {
                resolvedList.add(log);
            } else {
                actionPendingList.add(log);
            }
        }
    }

    private void updateSummaryCards() {
        int total = pendingAlertsList.size() + actionPendingList.size() + resolvedList.size() + falseList.size();

        totalCount.setText(String.valueOf(total));
        pendingAlertsCount.setText(String.valueOf(pendingAlertsList.size()));
        actionPendingCount.setText(String.valueOf(actionPendingList.size()));
        resolvedCount.setText(String.valueOf(resolvedList.size()));
        falseCount.setText(String.valueOf(falseList.size()));

        double totalFuelAmount = calculateTotalFuel(pendingAlertsList) +
                calculateTheftLogFuel(actionPendingList) +
                calculateTheftLogFuel(resolvedList) +
                calculateTheftLogFuel(falseList);

        totalFuel.setText(String.format("%.1f L", totalFuelAmount));
        pendingAlertsFuel.setText(String.format("%.1f L", calculateTotalFuel(pendingAlertsList)));
        actionPendingFuel.setText(String.format("%.1f L", calculateTheftLogFuel(actionPendingList)));
        resolvedFuel.setText(String.format("%.1f L", calculateTheftLogFuel(resolvedList)));
        falseFuel.setText(String.format("%.1f L", calculateTheftLogFuel(falseList)));
    }

    // Add the remaining methods from the previous version...
    private void filterData(String filter) {
        currentFilter = filter;
        currentPage = 1;

        filteredData.clear();

        String title = "";
        switch (filter) {
            case "ALL":
                filteredData.addAll(pendingAlertsList);
                filteredData.addAll(actionPendingList);
                filteredData.addAll(resolvedList);
                filteredData.addAll(falseList);
                title = "All Records";
                break;
            case "PENDING_ALERTS":
                filteredData.addAll(pendingAlertsList);
                title = "Pending Alerts";
                break;
            case "ACTION_PENDING":
                filteredData.addAll(actionPendingList);
                title = "Action Pending";
                break;
            case "RESOLVED":
                filteredData.addAll(resolvedList);
                title = "Resolved Thefts";
                break;
            case "FALSE":
                filteredData.addAll(falseList);
                title = "False Alerts";
                break;
        }

        filterTitle.setText(title);
        recordCountText.setText(filteredData.size() + " records");

        filterHeader.setVisibility(android.view.View.VISIBLE);
        if (filteredData.size() > ITEMS_PER_PAGE) {
            paginationHeader.setVisibility(android.view.View.VISIBLE);
        } else {
            paginationHeader.setVisibility(android.view.View.GONE);
        }

        displayCurrentFilter();
    }

    private void clearFilter() {
        currentFilter = "ALL";
        filterHeader.setVisibility(android.view.View.GONE);
        paginationHeader.setVisibility(android.view.View.GONE);
        dataContainer.removeAllViews();
    }

    // Update the displayCurrentFilter method to handle button states
    private void displayCurrentFilter() {
        dataContainer.removeAllViews();

        if (filteredData.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("📭 No records found for the selected filter and date range");
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.setGravity(android.view.Gravity.CENTER);
            dataContainer.addView(emptyView);
            return;
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) filteredData.size() / ITEMS_PER_PAGE);
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredData.size());

        // Update pagination info and button states
        pageInfo.setText("Page " + currentPage + " of " + totalPages);

        // Enable/disable and style buttons based on state
        boolean canGoPrev = currentPage > 1;
        boolean canGoNext = currentPage < totalPages;

        prevBtn.setEnabled(canGoPrev);
        nextBtn.setEnabled(canGoNext);

        // Style enabled/disabled buttons
        if (canGoPrev) {
            prevBtn.setTextColor(getResources().getColor(R.color.text_primary));
            prevBtn.setAlpha(1.0f);
        } else {
            prevBtn.setTextColor(getResources().getColor(R.color.text_hint));
            prevBtn.setAlpha(0.6f);
        }

        if (canGoNext) {
            nextBtn.setTextColor(getResources().getColor(R.color.text_primary));
            nextBtn.setAlpha(1.0f);
        } else {
            nextBtn.setTextColor(getResources().getColor(R.color.text_hint));
            nextBtn.setAlpha(0.6f);
        }

        // Display items for current page
        for (int i = startIndex; i < endIndex; i++) {
            Object item = filteredData.get(i);

            if (item instanceof TheftAlert) {
                displayTheftAlert((TheftAlert) item);
            } else if (item instanceof TheftLog) {
                displayTheftLog((TheftLog) item);
            }
        }
    }


    // Update the displayTheftAlert method to ensure clean layout
    private void displayTheftAlert(TheftAlert alert) {
        android.view.View alertView = getLayoutInflater().inflate(R.layout.item_theft_alert, null);

        // Set proper margins for spacing between cards
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8); // Top and bottom margins for spacing
        alertView.setLayoutParams(params);

        TextView vehicleName = alertView.findViewById(R.id.vehicle_name);
        TextView fuelAmount = alertView.findViewById(R.id.fuel_amount);
        TextView timestamp = alertView.findViewById(R.id.timestamp);
        TextView location = alertView.findViewById(R.id.location);
        android.widget.Button markTheftBtn = alertView.findViewById(R.id.mark_theft_btn);
        android.widget.Button markFalseBtn = alertView.findViewById(R.id.mark_false_btn);

        vehicleName.setText(alert.getCleanVehicleName());
        fuelAmount.setText(alert.fuelTheft);
        timestamp.setText(alert.getFormattedTimestamp());
        location.setText(alert.getFormattedLocation());

        markTheftBtn.setOnClickListener(v -> markAsTheft(alert));
        markFalseBtn.setOnClickListener(v -> markAsFalse(alert));

        dataContainer.addView(alertView);
    }

    // Update the displayTheftLog method to ensure clean layout
    private void displayTheftLog(TheftLog log) {
        boolean showResolveButton = currentFilter.equals("ACTION_PENDING");

        android.view.View logView = getLayoutInflater().inflate(
                showResolveButton ? R.layout.item_theft_pending : R.layout.item_theft_log, null);

        // Set proper margins for spacing between cards
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8); // Top and bottom margins for spacing
        logView.setLayoutParams(params);

        TextView vehicleName = logView.findViewById(R.id.vehicle_name);
        TextView fuelAmount = logView.findViewById(R.id.fuel_amount);
        TextView timestamp = logView.findViewById(R.id.timestamp);
        TextView status = logView.findViewById(R.id.status);
        TextView comments = logView.findViewById(R.id.comments);

        vehicleName.setText(log.getCleanVehicleName());
        fuelAmount.setText(log.fuelTheft);
        timestamp.setText(log.fuelTheftDate != null ? log.fuelTheftDate.split(" ")[0] : "N/A");
        status.setText(log.status != null ? log.status : getStatusFromLog(log));
        comments.setText(log.investigationComments != null ? log.investigationComments : "No comments");

        if (showResolveButton) {
            android.widget.Button resolveBtn = logView.findViewById(R.id.resolve_btn);
            if (resolveBtn != null) {
                resolveBtn.setOnClickListener(v -> resolveTheft(log));
            }
        }

        dataContainer.addView(logView);
    }


    private String getStatusFromLog(TheftLog log) {
        if (!log.isActualTheft) return "False Alert";
        if (log.isResolved) return "Resolved";
        return "Action Pending";
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            displayCurrentFilter();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredData.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            displayCurrentFilter();
        }
    }


    // COMPLETE CORRECTED markAsTheft method - NO API call without dialog
    private void markAsTheft(TheftAlert alert) {
        // Log to ensure this method is called
        Log.d("FuelTheftActivity", "markAsTheft called for vehicle: " + alert.vehicleName);

        // MANDATORY: Show dialog first - NO direct API call
        showJustificationDialog(
                "🚨 Confirm Theft",
                "Vehicle: " + alert.getCleanVehicleName() + "\nFuel: " + alert.fuelTheft + "\n\nThis will mark this alert as a CONFIRMED THEFT.",
                "Please provide remarks/justification for marking this as theft:",
                "Example: Security footage confirmed unauthorized fuel extraction",
                justification -> {
                    // This callback ONLY runs after user provides justification
                    Log.d("FuelTheftActivity", "User provided justification: " + justification);

                    // NOW create the request with user's remarks
                    performMarkAsTheft(alert, justification);
                }
        );

        // NO API call here - everything must go through dialog callback
    }

    // COMPLETE CORRECTED markAsFalse method - NO API call without dialog
    private void markAsFalse(TheftAlert alert) {
        // Log to ensure this method is called
        Log.d("FuelTheftActivity", "markAsFalse called for vehicle: " + alert.vehicleName);

        // MANDATORY: Show dialog first - NO direct API call
        showJustificationDialog(
                "❌ Mark as False Alert",
                "Vehicle: " + alert.getCleanVehicleName() + "\nFuel: " + alert.fuelTheft + "\n\nThis will mark this alert as a FALSE ALERT.",
                "Please provide remarks/justification for marking this as false alert:",
                "Example: Vehicle was refueling during this time / Sensor malfunction detected",
                justification -> {
                    // This callback ONLY runs after user provides justification
                    Log.d("FuelTheftActivity", "User provided justification: " + justification);

                    // NOW create the request with user's remarks
                    performMarkAsFalse(alert, justification);
                }
        );

        // NO API call here - everything must go through dialog callback
    }

    // COMPLETE CORRECTED resolveTheft method - NO API call without dialog
    private void resolveTheft(TheftLog log) {
        // Log to ensure this method is called
        Log.d("FuelTheftActivity", "resolveTheft called for vehicle: " + log.vehicleName);

        // MANDATORY: Show dialog first - NO direct API call
        showJustificationDialog(
                "🔧 Resolve Theft",
                "Vehicle: " + log.getCleanVehicleName() + "\nFuel: " + log.fuelTheft + "\nDate: " + (log.fuelTheftDate != null ? log.fuelTheftDate.split(" ")[0] : "N/A") + "\n\nThis will mark this theft as RESOLVED.",
                "Please provide remarks describing the action taken to resolve this theft:",
                "Example: Fuel tank security enhanced / Employee counseled / Security measures implemented",
                actionTaken -> {
                    // This callback ONLY runs after user provides action details
                    Log.d("FuelTheftActivity", "User provided action taken: " + actionTaken);

                    // NOW resolve with user's remarks
                    performResolveTheft(log, actionTaken);
                }
        );

        // NO API call here - everything must go through dialog callback
    }

    // SEPARATE method to perform actual mark as theft API call
    private void performMarkAsTheft(TheftAlert alert, String userRemarks) {
        CreateLogRequest request = new CreateLogRequest();
        request.vehicleCategory = alert.vehicleCategory;
        request.vehicleName = alert.vehicleName;
        request.vehicleType = alert.vehicleType;
        request.fuelTheft = alert.fuelTheft;

        if (alert.timestamp != null && !alert.timestamp.isEmpty()) {
            request.setFuelTheftDateFromTimestamp(alert.timestamp);
        } else {
            request.setFuelTheftDate(selectedFromDate);
        }

        request.fuelTheftLocation = alert.location != null ? alert.location : "NULL";
        request.investigationComments = userRemarks; // User's remarks are MANDATORY
        request.status = "Confirmed Theft";
        request.isActualTheft = true;

        Log.d("FuelTheftActivity", "API Call - Marking as theft with remarks: '" + userRemarks + "'");

        // Show loading
        Toast.makeText(this, "⏳ Processing with your remarks...", Toast.LENGTH_SHORT).show();

        repository.markAlert(request, new FuelTheftRepository.DataCallback<TheftLog>() {
            @Override
            public void onSuccess(TheftLog result) {
                runOnUiThread(() -> {
                    Toast.makeText(FuelTheftActivity.this, "✅ Marked as theft with remarks", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("FuelTheftActivity", "Error marking as theft: " + error);
                    Toast.makeText(FuelTheftActivity.this, "❌ Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // SEPARATE method to perform actual mark as false API call
    private void performMarkAsFalse(TheftAlert alert, String userRemarks) {
        CreateLogRequest request = new CreateLogRequest();
        request.vehicleCategory = alert.vehicleCategory;
        request.vehicleName = alert.vehicleName;
        request.vehicleType = alert.vehicleType;
        request.fuelTheft = alert.fuelTheft;

        if (alert.timestamp != null && !alert.timestamp.isEmpty()) {
            request.setFuelTheftDateFromTimestamp(alert.timestamp);
        } else {
            request.setFuelTheftDate(selectedFromDate);
        }

        request.fuelTheftLocation = alert.location != null ? alert.location : "NULL";
        request.investigationComments = userRemarks; // User's remarks are MANDATORY
        request.status = "False Alert";
        request.isActualTheft = false;

        Log.d("FuelTheftActivity", "API Call - Marking as false with remarks: '" + userRemarks + "'");

        // Show loading
        Toast.makeText(this, "⏳ Processing with your remarks...", Toast.LENGTH_SHORT).show();

        repository.markAlert(request, new FuelTheftRepository.DataCallback<TheftLog>() {
            @Override
            public void onSuccess(TheftLog result) {
                runOnUiThread(() -> {
                    Toast.makeText(FuelTheftActivity.this, "✅ Marked as false alert with remarks", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("FuelTheftActivity", "Error marking as false: " + error);
                    Toast.makeText(FuelTheftActivity.this, "❌ Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // SEPARATE method to perform actual resolve theft API call
    private void performResolveTheft(TheftLog log, String actionRemarks) {
        String dateForAPI = DateUtils.formatForAPI(log.fuelTheftDate);

        Log.d("FuelTheftActivity", "API Call - Resolving with action remarks: '" + actionRemarks + "'");

        // Show loading
        Toast.makeText(this, "⏳ Processing resolution with your remarks...", Toast.LENGTH_SHORT).show();

        repository.resolveTheft(
                log.vehicleName,
                dateForAPI,
                actionRemarks, // User's action remarks are MANDATORY
                new FuelTheftRepository.DataCallback<TheftLog>() {
                    @Override
                    public void onSuccess(TheftLog result) {
                        runOnUiThread(() -> {
                            Toast.makeText(FuelTheftActivity.this, "✅ Theft resolved with action remarks", Toast.LENGTH_SHORT).show();
                            loadData();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.e("FuelTheftActivity", "Error resolving theft: " + error);
                            Toast.makeText(FuelTheftActivity.this, "❌ Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    // ENHANCED justification dialog - MORE STRICT validation
    private void showJustificationDialog(String title, String message, String inputHint, String placeholder, JustificationCallback callback) {
        Log.d("FuelTheftActivity", "Showing mandatory justification dialog: " + title);

        // Create custom dialog layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(24, 16, 24, 16);

        // Message text
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextSize(14);
        messageView.setTextColor(getResources().getColor(R.color.text_primary));
        messageView.setPadding(0, 0, 0, 16);
        dialogLayout.addView(messageView);

        // Input hint
        TextView hintView = new TextView(this);
        hintView.setText(inputHint);
        hintView.setTextSize(12);
        hintView.setTextColor(getResources().getColor(R.color.text_secondary));
        hintView.setTypeface(hintView.getTypeface(), android.graphics.Typeface.BOLD);
        hintView.setPadding(0, 8, 0, 8);
        dialogLayout.addView(hintView);

        // Input field
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(placeholder);
        input.setMinLines(3); // Increased minimum lines
        input.setMaxLines(5); // Increased maximum lines
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        input.setPadding(16, 16, 16, 16);
        input.setBackground(getResources().getDrawable(R.drawable.date_input_background));
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        dialogLayout.addView(input);

        // Character counter
        TextView counterView = new TextView(this);
        counterView.setText("0/500 characters");
        counterView.setTextSize(10);
        counterView.setTextColor(getResources().getColor(R.color.text_hint));
        counterView.setPadding(0, 4, 0, 8);
        dialogLayout.addView(counterView);

        // Add character counter listener
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                counterView.setText(s.length() + "/500 characters");
                if (s.length() > 500) {
                    counterView.setTextColor(getResources().getColor(R.color.error_red));
                } else {
                    counterView.setTextColor(getResources().getColor(R.color.text_hint));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // MANDATORY notice - more prominent
        TextView requirementView = new TextView(this);
        requirementView.setText("🔒 MANDATORY: Remarks are required before any update. This will be logged for audit purposes.");
        requirementView.setTextSize(12);
        requirementView.setTextColor(getResources().getColor(R.color.error_red));
        requirementView.setTypeface(requirementView.getTypeface(), android.graphics.Typeface.BOLD);
        requirementView.setPadding(0, 12, 0, 0);
        dialogLayout.addView(requirementView);

        // Create dialog with stricter settings
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogLayout)
                .setPositiveButton("✅ Submit with Remarks", null) // Set to null initially
                .setNegativeButton("❌ Cancel", (d, which) -> {
                    Log.d("FuelTheftActivity", "User cancelled - No API call made");
                    Toast.makeText(FuelTheftActivity.this, "❌ Action cancelled - No changes made", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // CANNOT dismiss without action
                .create();

        dialog.show();

        // Override positive button with STRICT validation
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String remarks = input.getText().toString().trim();

            Log.d("FuelTheftActivity", "Validating user remarks: '" + remarks + "' (length: " + remarks.length() + ")");

            // STRICT Validation 1: Empty check
            if (remarks.isEmpty()) {
                input.setError("❌ Remarks are MANDATORY");
                input.requestFocus();
                Toast.makeText(FuelTheftActivity.this, "⚠️ You MUST provide remarks before proceeding", Toast.LENGTH_LONG).show();
                return;
            }

            // STRICT Validation 2: Minimum length (increased to 15)
            if (remarks.length() < 15) {
                input.setError("❌ Please provide detailed remarks (minimum 15 characters)");
                input.requestFocus();
                Toast.makeText(FuelTheftActivity.this, "⚠️ Remarks too short. Please provide more details (minimum 15 characters)", Toast.LENGTH_LONG).show();
                return;
            }

            // STRICT Validation 3: Maximum length check
            if (remarks.length() > 500) {
                input.setError("❌ Remarks too long (maximum 500 characters)");
                input.requestFocus();
                Toast.makeText(FuelTheftActivity.this, "⚠️ Please keep remarks under 500 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // STRICT Validation 4: Check for meaningful content (not just spaces/dots)
            if (remarks.replaceAll("[\\s\\.]", "").length() < 10) {
                input.setError("❌ Please provide meaningful remarks");
                input.requestFocus();
                Toast.makeText(FuelTheftActivity.this, "⚠️ Please provide meaningful remarks, not just spaces or dots", Toast.LENGTH_LONG).show();
                return;
            }

            // ALL validation passed - proceed with API call
            Log.d("FuelTheftActivity", "✅ Remarks validation passed. Proceeding with API call...");
            dialog.dismiss();

            // Show confirmation
            Toast.makeText(FuelTheftActivity.this, "📝 Remarks recorded. Making API call...", Toast.LENGTH_SHORT).show();

            // Execute the callback with validated remarks
            callback.onJustificationProvided(remarks);
        });

        // Style the buttons
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.success_green));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.error_red));

        // Auto-focus and show keyboard
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Log.d("FuelTheftActivity", "✅ Mandatory justification dialog displayed");
    }

    // Interface remains the same
    private interface JustificationCallback {
        void onJustificationProvided(String justification);
    }


    private double calculateTotalFuel(List<TheftAlert> alerts) {
        double total = 0.0;
        for (TheftAlert alert : alerts) {
            total += alert.getFuelAmount();
        }
        return total;
    }

    private double calculateTheftLogFuel(List<TheftLog> logs) {
        double total = 0.0;
        for (TheftLog log : logs) {
            total += log.getFuelAmount();
        }
        return total;
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