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
    private static final int PHOTO_TYPE_FRONT = 1;
    private static final int PHOTO_TYPE_BACK = 2;
    private static final int PHOTO_TYPE_DRIVER = 3;

    private TextInputEditText fullNameInput, licenseNumberInput, phoneNumberInput,
            aadharCardInput, licenseExpiryInput, dateOfJoinInput,
            userIdInput, passwordInput;

    private ImageView licensePhotoFrontPreview, licensePhotoBackPreview, driverPhotoPreview;
    private View uploadOverlayFront, uploadOverlayBack, uploadOverlayDriver;

    private Button captureLicenseFrontBtn, uploadLicenseFrontBtn;
    private Button captureLicenseBackBtn, uploadLicenseBackBtn;
    private Button captureDriverPhotoBtn, uploadDriverPhotoBtn;
    private Button submitBtn;

    private String selectedLicenseExpiry = "";
    private String selectedDateOfJoin = "";
    private byte[] licensePhotoFrontBytes;
    private byte[] licensePhotoBackBytes;
    private byte[] driverPhotoBytes;

    private int currentPhotoType = 0;

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
        userIdInput = view.findViewById(R.id.driver_user_id);
        passwordInput = view.findViewById(R.id.driver_password);

        licensePhotoFrontPreview = view.findViewById(R.id.license_photo_front_preview);
        licensePhotoBackPreview = view.findViewById(R.id.license_photo_back_preview);
        driverPhotoPreview = view.findViewById(R.id.driver_photo_preview);

        uploadOverlayFront = view.findViewById(R.id.upload_overlay_front);
        uploadOverlayBack = view.findViewById(R.id.upload_overlay_back);
        uploadOverlayDriver = view.findViewById(R.id.upload_overlay_driver);

        captureLicenseFrontBtn = view.findViewById(R.id.capture_license_front_btn);
        uploadLicenseFrontBtn = view.findViewById(R.id.upload_license_front_btn);
        captureLicenseBackBtn = view.findViewById(R.id.capture_license_back_btn);
        uploadLicenseBackBtn = view.findViewById(R.id.upload_license_back_btn);
        captureDriverPhotoBtn = view.findViewById(R.id.capture_driver_photo_btn);
        uploadDriverPhotoBtn = view.findViewById(R.id.upload_driver_photo_btn);

        submitBtn = view.findViewById(R.id.submit_driver_btn);
    }

    private void setupValidations() {
        // Phone validation
        phoneNumberInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() != 10) {
                    phoneNumberInput.setError("Must be 10 digits");
                } else {
                    phoneNumberInput.setError(null);
                }
            }
        });

        // Aadhar validation
        aadharCardInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && s.length() != 12) {
                    aadharCardInput.setError("Must be 12 digits");
                } else {
                    aadharCardInput.setError(null);
                }
            }
        });

        // License validation
        licenseNumberInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
        // Camera permission
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
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
                            displayImage(bitmap, currentPhotoType);
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
                            displayImage(bitmap, currentPhotoType);
                        } else {
                            Toast.makeText(requireContext(), "❌ Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void displayImage(Bitmap bitmap, int photoType) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] bytes = stream.toByteArray();

        switch (photoType) {
            case PHOTO_TYPE_FRONT:
                licensePhotoFrontPreview.setImageBitmap(bitmap);
                uploadOverlayFront.setVisibility(View.GONE);
                licensePhotoFrontBytes = bytes;
                Toast.makeText(requireContext(), "✅ License front uploaded", Toast.LENGTH_SHORT).show();
                break;
            case PHOTO_TYPE_BACK:
                licensePhotoBackPreview.setImageBitmap(bitmap);
                uploadOverlayBack.setVisibility(View.GONE);
                licensePhotoBackBytes = bytes;
                Toast.makeText(requireContext(), "✅ License back uploaded", Toast.LENGTH_SHORT).show();
                break;
            case PHOTO_TYPE_DRIVER:
                driverPhotoPreview.setImageBitmap(bitmap);
                uploadOverlayDriver.setVisibility(View.GONE);
                driverPhotoBytes = bytes;
                Toast.makeText(requireContext(), "✅ Driver photo uploaded", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupClickListeners() {
        licenseExpiryInput.setOnClickListener(v -> showDatePicker(true));
        dateOfJoinInput.setOnClickListener(v -> showDatePicker(false));

        // License Front
        captureLicenseFrontBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_FRONT;
            openCamera();
        });
        uploadLicenseFrontBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_FRONT;
            openGallery();
        });

        // License Back
        captureLicenseBackBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_BACK;
            openCamera();
        });
        uploadLicenseBackBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_BACK;
            openGallery();
        });

        // Driver Photo
        captureDriverPhotoBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_DRIVER;
            openCamera();
        });
        uploadDriverPhotoBtn.setOnClickListener(v -> {
            currentPhotoType = PHOTO_TYPE_DRIVER;
            openGallery();
        });

        submitBtn.setOnClickListener(v -> submitDriver());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker(boolean isLicenseExpiry) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomDatePickerDialogTheme,
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
        // Validate all inputs
        String fullName = fullNameInput.getText().toString().trim();
        String licenseNumber = licenseNumberInput.getText().toString().trim();
        String phoneNumber = phoneNumberInput.getText().toString().trim();
        String aadharCard = aadharCardInput.getText().toString().trim();
        String userId = userIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full name is required");
            fullNameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() != 10) {
            phoneNumberInput.setError("Valid 10-digit phone number required");
            phoneNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(licenseNumber) || licenseNumber.length() < 10) {
            licenseNumberInput.setError("Valid license number required");
            licenseNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(aadharCard) || aadharCard.length() != 12) {
            aadharCardInput.setError("Valid 12-digit Aadhar required");
            aadharCardInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(selectedLicenseExpiry)) {
            Toast.makeText(requireContext(), "⚠️ License expiry date required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            userIdInput.setError("User ID required for login");
            userIdInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        if (licensePhotoFrontBytes == null) {
            Toast.makeText(requireContext(), "⚠️ License front photo required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (licensePhotoBackBytes == null) {
            Toast.makeText(requireContext(), "⚠️ License back photo required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (driverPhotoBytes == null) {
            Toast.makeText(requireContext(), "⚠️ Driver photo required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        submitBtn.setEnabled(false);
        submitBtn.setText("Adding Driver...");
        Toast.makeText(requireContext(), "⏳ Adding driver and creating login...", Toast.LENGTH_SHORT).show();

        // Call API with all 3 photos
        repository.onboardDriver(
                fullName,
                phoneNumber,
                licenseNumber,
                selectedLicenseExpiry,
                selectedDateOfJoin,
                aadharCard,
                licensePhotoFrontBytes,
                licensePhotoBackBytes,
                driverPhotoBytes,
                userId,
                password,
                new UserManagementRepository.DriverCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            submitBtn.setEnabled(true);
                            submitBtn.setText("✅ Add Driver");
                            Toast.makeText(requireContext(),
                                    "✅ " + message + "\nDriver: " + fullName + "\nLogin ID: " + userId,
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
        userIdInput.setText("");
        passwordInput.setText("");

        licensePhotoFrontPreview.setImageResource(R.drawable.ic_image_placeholder);
        licensePhotoBackPreview.setImageResource(R.drawable.ic_image_placeholder);
        driverPhotoPreview.setImageResource(R.drawable.ic_image_placeholder);

        uploadOverlayFront.setVisibility(View.VISIBLE);
        uploadOverlayBack.setVisibility(View.VISIBLE);
        uploadOverlayDriver.setVisibility(View.VISIBLE);

        licensePhotoFrontBytes = null;
        licensePhotoBackBytes = null;
        driverPhotoBytes = null;
        selectedLicenseExpiry = "";
        selectedDateOfJoin = "";
    }
}
