package com.shashi.castlematic.features.user_management;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.shashi.castlematic.R;
import com.shashi.castlematic.features.user_management.models.UserManagementModels.*;
import com.shashi.castlematic.features.user_management.repository.UserManagementRepository;

public class AddUserFragment extends Fragment {

    private static final String TAG = "AddUserFragment";

    private TextInputEditText fullNameInput, userIdInput, phoneInput, passwordInput;
    private Spinner roleSpinner;
    private Button submitBtn;

    private UserManagementRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_user, container, false);

        repository = new UserManagementRepository(requireContext());

        initializeViews(view);
        setupRoleSpinner();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        fullNameInput = view.findViewById(R.id.user_full_name);
        userIdInput = view.findViewById(R.id.user_id);
        phoneInput = view.findViewById(R.id.user_phone);
        passwordInput = view.findViewById(R.id.user_password);
        roleSpinner = view.findViewById(R.id.user_role_spinner);
        submitBtn = view.findViewById(R.id.submit_user_btn);
    }

    private void setupRoleSpinner() {
        // API expects: "user", "admin", "super admin"
        String[] roles = {"user", "admin", "super admin"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        submitBtn.setOnClickListener(v -> submitUser());
    }

    private void submitUser() {
        // Validate inputs
        String fullName = fullNameInput.getText().toString().trim();
        String userId = userIdInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full name is required");
            fullNameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            userIdInput.setError("User ID is required");
            userIdInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return;
        }

        if (phone.length() != 10) {
            phoneInput.setError("Phone number must be 10 digits");
            phoneInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        // Show loading
        submitBtn.setEnabled(false);
        submitBtn.setText("Adding User...");
        Toast.makeText(requireContext(), "⏳ Adding user...", Toast.LENGTH_SHORT).show();

        // Call API
        repository.onboardUser(
                userId,
                fullName,
                role,
                password,
                new UserManagementRepository.UserCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            submitBtn.setEnabled(true);
                            submitBtn.setText("✅ Add User");
                            Toast.makeText(requireContext(),
                                    "✅ " + message + "\n" + fullName + " - " + role,
                                    Toast.LENGTH_LONG).show();
                            clearForm();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            submitBtn.setEnabled(true);
                            submitBtn.setText("✅ Add User");
                            Toast.makeText(requireContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private void clearForm() {
        fullNameInput.setText("");
        userIdInput.setText("");
        phoneInput.setText("");
        passwordInput.setText("");
        roleSpinner.setSelection(0);
    }
}
