package com.shashi.castlematic.features.user_management.models;

import com.google.gson.annotations.SerializedName;

public class UserManagementModels {

    // Driver model
    public static class Driver {
        @SerializedName("full_name")
        public String fullName;

        @SerializedName("license_number")
        public String licenseNumber;

        @SerializedName("license_expiry")
        public String licenseExpiry;

        @SerializedName("license_photo_url")
        public String licensePhotoUrl;

        @SerializedName("date_of_join")
        public String dateOfJoin;

        @SerializedName("status")
        public String status = "active";

        public Driver() {}

        public Driver(String fullName, String licenseNumber, String licenseExpiry,
                      String licensePhotoUrl, String dateOfJoin) {
            this.fullName = fullName;
            this.licenseNumber = licenseNumber;
            this.licenseExpiry = licenseExpiry;
            this.licensePhotoUrl = licensePhotoUrl;
            this.dateOfJoin = dateOfJoin;
        }
    }

    // User model
    public static class User {
        @SerializedName("full_name")
        public String fullName;

        @SerializedName("emp_id")
        public String empId;

        @SerializedName("phone_number")
        public String phoneNumber;

        @SerializedName("role")
        public String role;

        @SerializedName("username")
        public String username;

        @SerializedName("password")
        public String password;

        @SerializedName("status")
        public String status = "active";

        public User() {}

        public User(String fullName, String empId, String phoneNumber, String role) {
            this.fullName = fullName;
            this.empId = empId;
            this.phoneNumber = phoneNumber;
            this.role = role;
        }
    }

    // Response models
    public static class DriverResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("message")
        public String message;

        @SerializedName("driver_id")
        public String driverId;
    }

    public static class UserResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("message")
        public String message;

        @SerializedName("user_id")
        public String userId;
    }

    // User Role Enum
    public enum UserRole {
        USER("user", "User"),
        ADMIN("admin", "Admin"),
        SUPER_ADMIN("super admin", "Super Admin");

        private final String value;
        private final String displayName;

        UserRole(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String[] getAllRoles() {
            UserRole[] roles = values();
            String[] displayNames = new String[roles.length];
            for (int i = 0; i < roles.length; i++) {
                displayNames[i] = roles[i].displayName;
            }
            return displayNames;
        }
    }
}
