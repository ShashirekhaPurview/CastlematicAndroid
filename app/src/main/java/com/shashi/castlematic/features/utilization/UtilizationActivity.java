package com.shashi.castlematic.features.utilization;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.shashi.castlematic.R;
import com.shashi.castlematic.SessionManager;
import com.shashi.castlematic.LoginActivity;
import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.core.network.AuthManager;
import com.shashi.castlematic.features.utilization.repository.UtilizationRepository;
import com.shashi.castlematic.features.utilization.models.UtilizationModels.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UtilizationActivity extends AppCompatActivity {

    private static final String TAG = "UtilizationActivity";
    private static final int ITEMS_PER_PAGE = 10;

    private SessionManager sessionManager;
    private UtilizationRepository repository;

    // Date range
    private LinearLayout fromDateContainer, toDateContainer;
    private TextView fromDateText, toDateText;
    private String fromDate, toDate;

    // Filters
    private Spinner measurementTypeSpinner, ownershipSpinner;
    private Button loadDataBtn;
    private EditText vehicleSearchInput;

    // Summary
    private TextView overallUtilization, overallAvailability;

    // Pagination
    private LinearLayout paginationHeader;
    private Button prevPageBtn, nextPageBtn;
    private TextView pageInfoText;
    private int currentPage = 1;

    // Vehicle list
    private LinearLayout vehiclesContainer;

    // Data
    private VehicleStatsResponse currentData;
    private List<Vehicle> allVehicles = new ArrayList<>();
    private List<Vehicle> filteredVehicles = new ArrayList<>();
    private String currentMeasurementType = "Both";
    private String currentOwnership = "Both";

    // Driver list with search support
    private String[] driverList = {
            "Select Driver",
            "Ravi Kumar - D001",
            "Suresh Patel - D002",
            "Rajesh Singh - D003",
            "Amit Sharma - D004",
            "Vijay Reddy - D005",
            "Prakash Rao - D006",
            "Kiran Kumar - D007",
            "Manoj Verma - D008",
            "Santosh Naik - D009",
            "Ramesh Babu - D010"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utilization);

        ApiClient.initialize(this);
        setupAuthentication();

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        sessionManager.updateLastActivity();

        repository = new UtilizationRepository();

        // Set default date range (yesterday)
        setYesterdayRange();

        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupSpinners();
        setupSearch();

        // AUTO-LOAD DATA ON START
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
            getSupportActionBar().setTitle("Utilization vs Availability");
        }
    }

    private void initializeViews() {
        // Date range
        fromDateContainer = findViewById(R.id.from_date_container);
        toDateContainer = findViewById(R.id.to_date_container);
        fromDateText = findViewById(R.id.from_date_text);
        toDateText = findViewById(R.id.to_date_text);

        // Filters
        measurementTypeSpinner = findViewById(R.id.measurement_type_spinner);
        ownershipSpinner = findViewById(R.id.ownership_spinner);
        loadDataBtn = findViewById(R.id.load_data_btn);
        vehicleSearchInput = findViewById(R.id.vehicle_search);

        // Summary
        overallUtilization = findViewById(R.id.overall_utilization);
        overallAvailability = findViewById(R.id.overall_availability);

        // Pagination
        paginationHeader = findViewById(R.id.pagination_header);
        prevPageBtn = findViewById(R.id.prev_page_btn);
        nextPageBtn = findViewById(R.id.next_page_btn);
        pageInfoText = findViewById(R.id.page_info_text);

        // Vehicle list
        vehiclesContainer = findViewById(R.id.vehicles_container);

        updateDateDisplays();
    }

    private void setupSpinners() {
        // Measurement type spinner
        String[] measurementTypes = {"Both", "Hours", "KMs"};
        ArrayAdapter<String> measurementAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, measurementTypes);
        measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        measurementTypeSpinner.setAdapter(measurementAdapter);

        // Ownership spinner
        String[] ownershipTypes = {"Both", "Owned", "Hired"};
        ArrayAdapter<String> ownershipAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ownershipTypes);
        ownershipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ownershipSpinner.setAdapter(ownershipAdapter);
    }

    private void setupSearch() {
        vehicleSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVehicles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        // Date pickers
        fromDateContainer.setOnClickListener(v -> showDatePicker(true));
        toDateContainer.setOnClickListener(v -> showDatePicker(false));

        // Load data
        loadDataBtn.setOnClickListener(v -> {
            currentMeasurementType = measurementTypeSpinner.getSelectedItem().toString();
            currentOwnership = ownershipSpinner.getSelectedItem().toString();
            currentPage = 1;
            loadData();
        });

        // Pagination
        prevPageBtn.setOnClickListener(v -> previousPage());
        nextPageBtn.setOnClickListener(v -> nextPage());
    }

    private void setYesterdayRange() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // Yesterday

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        toDate = sdf.format(cal.getTime());
        fromDate = toDate; // Same date for single day
    }

    private void updateDateDisplays() {
        fromDateText.setText(fromDate);
        toDateText.setText(toDate);
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();

        // Set max date to yesterday
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long maxDate = calendar.getTimeInMillis();

        // Parse current date
        String currentDateStr = isFromDate ? fromDate : toDate;
        String[] dateParts = currentDateStr.split("-");
        calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2]));

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);

                    if (isFromDate) {
                        // From date cannot be after to date
                        if (selectedDate.compareTo(toDate) > 0) {
                            Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        fromDate = selectedDate;
                    } else {
                        // To date cannot be before from date
                        if (selectedDate.compareTo(fromDate) < 0) {
                            Toast.makeText(this, "To date cannot be before From date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        toDate = selectedDate;
                    }

                    updateDateDisplays();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Block today and future dates
        datePickerDialog.getDatePicker().setMaxDate(maxDate);

        // Set min date for To Date picker (should be >= From Date)
        if (!isFromDate) {
            try {
                String[] fromParts = fromDate.split("-");
                Calendar minCal = Calendar.getInstance();
                minCal.set(Integer.parseInt(fromParts[0]),
                        Integer.parseInt(fromParts[1]) - 1,
                        Integer.parseInt(fromParts[2]));
                datePickerDialog.getDatePicker().setMinDate(minCal.getTimeInMillis());
            } catch (Exception e) {
                Log.e(TAG, "Error setting min date", e);
            }
        }

        datePickerDialog.show();
    }

    private void loadData() {
        Toast.makeText(this, "🔄 Loading vehicle statistics...", Toast.LENGTH_SHORT).show();

        repository.getVehicleStats(fromDate, toDate, currentOwnership,
                new UtilizationRepository.DataCallback<VehicleStatsResponse>() {
                    @Override
                    public void onSuccess(VehicleStatsResponse response) {
                        runOnUiThread(() -> {
                            currentData = response;
                            processAndDisplayData();
                            Toast.makeText(UtilizationActivity.this,
                                    "✅ Data loaded successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(UtilizationActivity.this,
                                    "❌ Error: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error loading data: " + error);
                        });
                    }
                });
    }

    private void processAndDisplayData() {
        if (currentData == null) return;

        // Get the appropriate data based on measurement type filter
        StatsData dataToShow = null;

        if ("Hours".equals(currentMeasurementType)) {
            dataToShow = currentData.hours;
        } else if ("KMs".equals(currentMeasurementType)) {
            dataToShow = currentData.kms;
        } else {
            dataToShow = currentData.hours; // Default to hours for "Both"
        }

        if (dataToShow == null) {
            showEmptyState();
            return;
        }

        // Update summary
        updateSummary(dataToShow);

        // Extract all vehicles into flat list
        extractAllVehicles(dataToShow);

        // Apply search filter
        String searchQuery = vehicleSearchInput.getText().toString();
        filterVehicles(searchQuery);
    }

    private void extractAllVehicles(StatsData data) {
        allVehicles.clear();

        if (data.groups == null) return;

        for (VehicleGroup group : data.groups) {
            if (group.categories != null) {
                for (VehicleCategory category : group.categories) {
                    if (category.vehicles != null) {
                        allVehicles.addAll(category.vehicles);
                    }
                }
            }
        }
    }

    private void filterVehicles(String query) {
        filteredVehicles.clear();

        if (query == null || query.trim().isEmpty()) {
            filteredVehicles.addAll(allVehicles);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Vehicle vehicle : allVehicles) {
                if (vehicle.vehicleName.toLowerCase().contains(lowerQuery)) {
                    filteredVehicles.add(vehicle);
                }
            }
        }

        currentPage = 1;
        displayCurrentPage();
    }

    private void updateSummary(StatsData data) {
        overallUtilization.setText(data.overallUtilization + "%");
        overallAvailability.setText(data.overallAvailability + "%");
    }

    private void displayCurrentPage() {
        vehiclesContainer.removeAllViews();

        if (filteredVehicles.isEmpty()) {
            showEmptyState();
            paginationHeader.setVisibility(android.view.View.GONE);
            return;
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) filteredVehicles.size() / ITEMS_PER_PAGE);
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredVehicles.size());

        // Update pagination controls
        if (totalPages > 1) {
            paginationHeader.setVisibility(android.view.View.VISIBLE);
            pageInfoText.setText("Page " + currentPage + " of " + totalPages +
                    " (Showing " + (startIndex + 1) + "-" + endIndex + " of " + filteredVehicles.size() + ")");

            prevPageBtn.setEnabled(currentPage > 1);
            nextPageBtn.setEnabled(currentPage < totalPages);

            prevPageBtn.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
            nextPageBtn.setAlpha(currentPage < totalPages ? 1.0f : 0.5f);
        } else {
            paginationHeader.setVisibility(android.view.View.GONE);
        }

        // Display current page vehicles
        for (int i = startIndex; i < endIndex; i++) {
            displayVehicleCard(filteredVehicles.get(i));
        }
    }
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            displayCurrentPage();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) filteredVehicles.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            displayCurrentPage();
        }
    }

    private void displayVehicleCard(Vehicle vehicle) {
        android.view.View cardView = getLayoutInflater().inflate(R.layout.item_vehicle_utilization, null);

        // Set margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(params);

        // Get views
        TextView vehicleName = cardView.findViewById(R.id.vehicle_name);
        TextView utilizationPercent = cardView.findViewById(R.id.utilization_percent);
        TextView availabilityPercent = cardView.findViewById(R.id.availability_percent);
        Spinner shiftSpinner = cardView.findViewById(R.id.shift_spinner);
        Spinner driverSpinner = cardView.findViewById(R.id.driver_spinner);
        Button assignBtn = cardView.findViewById(R.id.assign_btn);

        // Set vehicle data
        vehicleName.setText(vehicle.getCleanVehicleName());
        utilizationPercent.setText(vehicle.vehicleUtilization + "%");
        availabilityPercent.setText(vehicle.vehicleAvailability + "%");

        // Setup shift spinner
        ArrayAdapter<String> shiftAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ShiftType.getAllShifts()
        );
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shiftSpinner.setAdapter(shiftAdapter);

        // Setup driver spinner - REPLACED WITH SEARCHABLE DIALOG
        ArrayAdapter<String> driverAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{vehicle.assignedDriver}
        );
        driverSpinner.setAdapter(driverAdapter);

        // FIXED: Use only touch listener (no setOnClickListener)
        driverSpinner.setFocusable(true);
        driverSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                showDriverSearchDialog(vehicle, driverSpinner);
            }
            return true; // Consume the event to prevent default spinner behavior
        });

        // Assign button click
        assignBtn.setOnClickListener(v -> {
            String selectedShift = shiftSpinner.getSelectedItem().toString();
            String selectedDriver = driverSpinner.getSelectedItem().toString();

            if ("Not Assigned".equals(selectedShift)) {
                Toast.makeText(this, "⚠️ Please select a shift", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("Select Driver".equals(selectedDriver)) {
                Toast.makeText(this, "⚠️ Please select a driver", Toast.LENGTH_SHORT).show();
                return;
            }

            assignShiftAndDriver(vehicle, selectedShift, selectedDriver);
        });

        vehiclesContainer.addView(cardView);
    }

    // UPDATED: Show searchable driver dialog with dark text
    // UPDATED: Show searchable driver dialog with assign button
    private void showDriverSearchDialog(Vehicle vehicle, Spinner driverSpinner) {
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_driver_search, null);

        EditText searchInput = dialogView.findViewById(R.id.driver_search_input);
        android.widget.ListView driverListView = dialogView.findViewById(R.id.driver_list);
        TextView selectedDriverText = dialogView.findViewById(R.id.selected_driver_text);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button assignButton = dialogView.findViewById(R.id.assign_driver_button);

        // Track selected driver
        final String[] selectedDriver = {null};

        // Create adapter with dark text layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_single_choice,
                new ArrayList<>(java.util.Arrays.asList(driverList))
        ) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                // Force dark text color
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                    textView.setTextSize(14);
                    textView.setPadding(16, 16, 16, 16);
                }
                return view;
            }
        };
        driverListView.setAdapter(adapter);

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = builder.setView(dialogView).create();

        // Search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // List item click - just select, don't assign yet
        driverListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDriver[0] = adapter.getItem(position);
            selectedDriverText.setText(selectedDriver[0]);
            assignButton.setEnabled(true);

            // Highlight selection
            driverListView.setItemChecked(position, true);
        });

        // Assign button click
        assignButton.setOnClickListener(v -> {
            if (selectedDriver[0] != null && !selectedDriver[0].equals("Select Driver")) {
                // Update spinner
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{selectedDriver[0]}
                );
                driverSpinner.setAdapter(newAdapter);

                // Update vehicle data
                vehicle.assignedDriver = selectedDriver[0];

                Toast.makeText(this, "✅ Driver assigned: " + selectedDriver[0], Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "⚠️ Please select a driver first", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Auto-focus search input
        searchInput.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void assignShiftAndDriver(Vehicle vehicle, String shift, String driver) {
        // Create custom dialog for better styling
        LinearLayout dialogView = new LinearLayout(this);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(24, 20, 24, 20);
        dialogView.setBackgroundColor(getResources().getColor(android.R.color.white));

        // Icon
        TextView iconView = new TextView(this);
        iconView.setText("✅");
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
        String message = "Assign " + shift + " with " + driver + " to " + vehicle.getCleanVehicleName() + "?";
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
                    performAssignment(vehicle, shift, driver);
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

    private void performAssignment(Vehicle vehicle, String shift, String driver) {
        Toast.makeText(this, "⏳ Assigning...", Toast.LENGTH_SHORT).show();

        // TODO: Implement actual API call when endpoint is available
        // ShiftAssignmentRequest request = new ShiftAssignmentRequest(
        //     vehicle.vehicleName, fromDate, shift, driver);
        // repository.assignShift(request, callback);

        // Simulated success
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this,
                    "✅ Successfully assigned " + shift + " with " + driver + " to " + vehicle.getCleanVehicleName(),
                    Toast.LENGTH_LONG).show();

            // Update local vehicle data
            vehicle.assignedShift = shift;
            vehicle.assignedDriver = driver;
        }, 800);
    }

    private void showEmptyState() {
        TextView emptyView = new TextView(this);

        String searchQuery = vehicleSearchInput.getText().toString();
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            emptyView.setText("🔍 No vehicles found matching \"" + searchQuery + "\"");
        } else {
            emptyView.setText("📭 No vehicle data found for the selected date range and filters");
        }

        emptyView.setPadding(16, 32, 16, 32);
        emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setTextSize(14);
        vehiclesContainer.addView(emptyView);
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

    // Inner class for searchable spinner adapter
    private class SearchableArrayAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> originalList;
        private List<String> filteredList;
        private DriverFilter filter;

        public SearchableArrayAdapter(android.content.Context context, int resource, String[] objects) {
            super(context, resource, objects);
            this.originalList = new ArrayList<>(java.util.Arrays.asList(objects));
            this.filteredList = new ArrayList<>(originalList);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public String getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new DriverFilter();
            }
            return filter;
        }

        private class DriverFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    results.values = originalList;
                    results.count = originalList.size();
                } else {
                    List<String> filtered = new ArrayList<>();
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (String item : originalList) {
                        if (item.toLowerCase().contains(filterPattern)) {
                            filtered.add(item);
                        }
                    }

                    results.values = filtered;
                    results.count = filtered.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        }
    }
}

