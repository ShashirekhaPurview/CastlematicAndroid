package com.shashi.castlematic.features.user_management;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.shashi.castlematic.R;
import com.shashi.castlematic.features.user_management.repository.UserManagementRepository;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Locale;

public class AddDriverFragment extends Fragment {

    private static final String TAG = "AddDriverFragment";

    private TextInputEditText fullNameInput, licenseNumberInput, phoneNumberInput,
            aadharCardInput, licenseExpiryInput, dateOfJoinInput;
    private ImageView licensePhotoPreview;
    private View uploadOverlay;
    private Button capturePhotoBtn, uploadPhotoBtn, submitBtn;

    private String selectedLicenseExpiry = "";
    private String selectedDateOfJoin = "";
    private byte[] licensePhotoBytes;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private UserManagementRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_driver, container, false);

        repository = new UserManagementRepository(requireContext());

        initializeViews(view);
        setupImagePickers();
        setupClickListeners();
        setupValidations();

        return view;
    }

    private void initializeViews(View view) {
        fullNameInput = view.findViewById(R.id.driver_full_name);
        licenseNumberInput = view.findViewById(R.id.driver_license_number);
        phoneNumberInput = view.findViewById(R.id.driver_phone_number);
        aadharCardInput = view.findViewById(R.id.driver_aadhar_card);
        licenseExpiryInput = view.findViewById(R.id.driver_license_expiry);
        dateOfJoinInput = view.findViewById(R.id.driver_date_of_join);
        licensePhotoPreview = view.findViewById(R.id.license_photo_preview);
        uploadOverlay = view.findViewById(R.id.upload_overlay);
        capturePhotoBtn = view.findViewById(R.id.capture_photo_btn);
        uploadPhotoBtn = view.findViewById(R.id.upload_photo_btn);
        submitBtn = view.findViewById(R.id.submit_driver_btn);
    }

    private void setupValidations() {
        // Phone number validation
        phoneNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() != 10) {
                    phoneNumberInput.setError("Must be 10 digits");
                } else {
                    phoneNumberInput.setError(null);
                }
            }
        });

        // Aadhar card validation
        aadharCardInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() != 12) {
                    aadharCardInput.setError("Must be 12 digits");
                } else {
                    aadharCardInput.setError(null);
                }
            }
        });

        // License number validation (typically 13-16 characters)
        licenseNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() < 10) {
                    licenseNumberInput.setError("Invalid license number");
                } else {
                    licenseNumberInput.setError(null);
                }
            }
        });
    }

    private void setupImagePickers() {
        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(), "❌ Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Gallery picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(), imageUri);
                            displayImage(bitmap);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading image from gallery", e);
                            Toast.makeText(requireContext(), "❌ Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Camera capture
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        if (bitmap != null) {
                            displayImage(bitmap);
                        } else {
                            Toast.makeText(requireContext(), "❌ Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void displayImage(Bitmap bitmap) {
        licensePhotoPreview.setImageBitmap(bitmap);
        uploadOverlay.setVisibility(View.GONE);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        licensePhotoBytes = stream.toByteArray();

        Toast.makeText(requireContext(), "✅ Photo uploaded", Toast.LENGTH_SHORT).show();
    }

    private void setupClickListeners() {
        licenseExpiryInput.setOnClickListener(v -> showDatePicker(true));
        dateOfJoinInput.setOnClickListener(v -> showDatePicker(false));
        capturePhotoBtn.setOnClickListener(v -> openCamera());
        uploadPhotoBtn.setOnClickListener(v -> openGallery());
        submitBtn.setOnClickListener(v -> submitDriver());
    }

    private void openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            // Request camera permission
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker(boolean isLicenseExpiry) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomDatePickerDialogTheme,  // Use custom theme
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    if (isLicenseExpiry) {
                        selectedLicenseExpiry = date;
                        licenseExpiryInput.setText(date);
                    } else {
                        selectedDateOfJoin = date;
                        dateOfJoinInput.setText(date);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (isLicenseExpiry) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }

        datePickerDialog.show();
    }

    private void submitDriver() {
        // Validate inputs
        String fullName = fullNameInput.getText().toString().trim();
        String licenseNumber = licenseNumberInput.getText().toString().trim();
        String phoneNumber = phoneNumberInput.getText().toString().trim();
        String aadharCard = aadharCardInput.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full name is required");
            fullNameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(licenseNumber)) {
            licenseNumberInput.setError("License number is required");
            licenseNumberInput.requestFocus();
            return;
        }

        if (licenseNumber.length() < 10) {
            licenseNumberInput.setError("Invalid license number format");
            licenseNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberInput.setError("Phone number is required");
            phoneNumberInput.requestFocus();
            return;
        }

        if (phoneNumber.length() != 10) {
            phoneNumberInput.setError("Phone number must be 10 digits");
            phoneNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(aadharCard)) {
            aadharCardInput.setError("Aadhar card is required");
            aadharCardInput.requestFocus();
            return;
        }

        if (aadharCard.length() != 12) {
            aadharCardInput.setError("Aadhar card must be 12 digits");
            aadharCardInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedLicenseExpiry)) {
            Toast.makeText(requireContext(), "⚠️ Please select license expiry date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (licensePhotoBytes == null) {
            Toast.makeText(requireContext(), "⚠️ Please upload license photo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        submitBtn.setEnabled(false);
        submitBtn.setText("Adding Driver...");
        Toast.makeText(requireContext(), "⏳ Adding driver...", Toast.LENGTH_SHORT).show();

        // Call API
        repository.onboardDriver(
                fullName,
                phoneNumber,
                licenseNumber,
                selectedLicenseExpiry,
                selectedDateOfJoin,
                aadharCard,
                licensePhotoBytes,
                new UserManagementRepository.DriverCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            submitBtn.setEnabled(true);
                            submitBtn.setText("✅ Add Driver");
                            Toast.makeText(requireContext(),
                                    "✅ " + message + "\n" + fullName,
                                    Toast.LENGTH_LONG).show();
                            clearForm();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            submitBtn.setEnabled(true);
                            submitBtn.setText("✅ Add Driver");
                            Toast.makeText(requireContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private void clearForm() {
        fullNameInput.setText("");
        licenseNumberInput.setText("");
        phoneNumberInput.setText("");
        aadharCardInput.setText("");
        licenseExpiryInput.setText("");
        dateOfJoinInput.setText("");
        licensePhotoPreview.setImageResource(R.drawable.ic_image_placeholder);
        uploadOverlay.setVisibility(View.VISIBLE);
        licensePhotoBytes = null;
        selectedLicenseExpiry = "";
        selectedDateOfJoin = "";
    }
}
