package com.shashi.castlematic.features.fuel_theft.models;

import com.google.gson.annotations.SerializedName;
import com.shashi.castlematic.core.utils.DateUtils;

import java.util.List;

// ALL FUEL THEFT MODELS IN ONE FILE
public class TheftModels {

    // ===== REQUEST MODELS =====
    public static class DateRequest {
        @SerializedName("fromDate")
        public String fromDate;

        @SerializedName("toDate")
        public String toDate;

        @SerializedName("ownedOrHired")
        public String ownedOrHired; // "both", "Owned", "Hired"

        @SerializedName("threshold")
        public int threshold = 10; // Add threshold parameter as per API

        public DateRequest(String fromDate, String toDate, String ownedOrHired) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.ownedOrHired = ownedOrHired.toLowerCase(); // Convert to lowercase for API
            this.threshold = 10; // Default threshold
        }
    }

    public static class CreateLogRequest {
        @SerializedName("clientId")
        public String clientId = "MEIL-5345";

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("vehicleType")
        public String vehicleType;

        @SerializedName("fuelTheft")
        public String fuelTheft;

        @SerializedName("fuelTheftDate")
        public String fuelTheftDate; // Must be in DD/MM/YYYY, HH:MM AM/PM format

        @SerializedName("fuelTheftLocation")
        public String fuelTheftLocation;

        @SerializedName("fuelType")
        public String fuelType = "diesel";

        @SerializedName("investigationComments")
        public String investigationComments;

        @SerializedName("status")
        public String status; // "Confirmed Theft" or "False Alert"

        @SerializedName("isActualTheft")
        public boolean isActualTheft;

        @SerializedName("isResolved")
        public boolean isResolved = false;

        // Helper method to set date with proper formatting
        public void setFuelTheftDate(String inputDate) {
            this.fuelTheftDate = DateUtils.formatForMarkAPI(inputDate);
        }

        // Helper method to set date from timestamp
        public void setFuelTheftDateFromTimestamp(String timestamp) {
            this.fuelTheftDate = DateUtils.timestampToMarkAPI(timestamp);
        }
    }


    public static class ResolveRequest {
        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("dateOfData")
        public String dateOfData; // Must be YYYY-MM-DD format

        @SerializedName("confirmedComments")
        public String confirmedComments;

        public ResolveRequest(String vehicleName, String dateOfData, String confirmedComments) {
            this.vehicleName = vehicleName;
            this.dateOfData = formatDateForAPI(dateOfData); // Format properly
            this.confirmedComments = confirmedComments;
        }

        // Helper method to ensure proper date format
        private String formatDateForAPI(String inputDate) {
            if (inputDate == null || inputDate.isEmpty()) {
                return inputDate;
            }

            // Remove any trailing commas or whitespace
            inputDate = inputDate.trim().replaceAll(",$", "");

            // If already in YYYY-MM-DD format, return as is
            if (inputDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return inputDate;
            }

            // If in DD/MM/YYYY format, convert to YYYY-MM-DD
            if (inputDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
                String[] parts = inputDate.split("/");
                return parts[2] + "-" + parts[1] + "-" + parts[0]; // YYYY-MM-DD
            }

            // If in DD-MM-YYYY format, convert to YYYY-MM-DD
            if (inputDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] parts = inputDate.split("-");
                return parts[2] + "-" + parts[1] + "-" + parts[0]; // YYYY-MM-DD
            }

            // Return as is if format not recognized
            return inputDate;
        }
    }

    // ===== RESPONSE MODELS =====
    public static class TheftAlert {
        @SerializedName("sno")
        public int sno;

        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("ownedOrHired")
        public String ownedOrHired; // "Owned" or "Hired"

        @SerializedName("vehicleType")
        public String vehicleType;

        @SerializedName("fuelTheft")
        public String fuelTheft; // "16.30 L"

        @SerializedName("theftAmount")
        public double theftAmount; // 1559.91

        @SerializedName("timestamp")
        public String timestamp; // "2025-09-12 16:27:11"

        @SerializedName("location")
        public String location;

        @SerializedName("fuelType")
        public String fuelType; // "diesel"

        @SerializedName("isFuelTheftConfirmed")
        public boolean isFuelTheftConfirmed = false;

        @SerializedName("isActualTheftAlert")
        public boolean isActualTheftAlert = true;

        @SerializedName("clientId")
        public String clientId;

        // Helper methods
        public double getFuelAmount() {
            try {
                return Double.parseDouble(fuelTheft.replace(" L", "").replace("L", "").trim());
            } catch (Exception e) {
                return 0.0;
            }
        }

        public String getCleanVehicleName() {
            return vehicleName != null ? vehicleName.toUpperCase() : "Unknown";
        }

        public String getFormattedTheftAmount() {
            return String.format("₹%.2f", theftAmount);
        }

        public String getOwnershipBadge() {
            return ownedOrHired != null ? ownedOrHired : "Unknown";
        }

        public String getFormattedLocation() {
            if (location != null && !location.trim().isEmpty()) {
                // Truncate long location names
                return location.length() > 30 ? location.substring(0, 27) + "..." : location;
            }
            return "Location not available";
        }

        public String getFormattedTimestamp() {
            if (timestamp != null && timestamp.contains(" ")) {
                return timestamp.split(" ")[0]; // Return date part only
            }
            return timestamp != null ? timestamp : "N/A";
        }
    }

    public static class TheftLog {
        @SerializedName("_id")
        public String id;

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("ownedOrHired")
        public String ownedOrHired; // Add this field

        @SerializedName("vehicleType")
        public String vehicleType;

        @SerializedName("fuelTheft")
        public String fuelTheft;

        @SerializedName("theftAmount")
        public double theftAmount; // Add this field

        @SerializedName("fuelTheftDate")
        public String fuelTheftDate;

        @SerializedName("fuelTheftLocation")
        public String fuelTheftLocation;

        @SerializedName("fuelType")
        public String fuelType;

        @SerializedName("investigationComments")
        public String investigationComments;

        @SerializedName("confirmedComments")
        public String confirmedComments;

        @SerializedName("status")
        public String status;

        @SerializedName("isActualTheft")
        public boolean isActualTheft;

        @SerializedName("isResolved")
        public boolean isResolved;

        @SerializedName("clientId")
        public String clientId;

        @SerializedName("createdAt")
        public String createdAt;

        // Helper methods
        public double getFuelAmount() {
            try {
                return Double.parseDouble(fuelTheft.replace(" L", "").replace("L", "").trim());
            } catch (Exception e) {
                return 0.0;
            }
        }

        public String getFormattedTheftAmount() {
            return String.format("₹%.2f", theftAmount);
        }

        public String getCleanVehicleName() {
            return vehicleName != null ? vehicleName.toUpperCase() : "Unknown";
        }

        public String getOwnershipBadge() {
            return ownedOrHired != null ? ownedOrHired : "Unknown";
        }
    }

    public static class VehicleCountResponse {
        @SerializedName("vehicles")
        public List<VehicleCount> vehicles;

        @SerializedName("totalVehicles")
        public int totalVehicles; // Add if API provides this

        @SerializedName("ownedCount")
        public int ownedCount; // Add if API provides this

        @SerializedName("hiredCount")
        public int hiredCount; // Add if API provides this
    }

    public static class VehicleCount {
        @SerializedName("ownedOrHired")
        public String ownedOrHired;

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("vehicleType")
        public String vehicleType; // Add if needed

        @SerializedName("count")
        public int count;
    }

    // ===== AUTHENTICATION MODELS =====
    public static class AuthRequest {
        @SerializedName("grant_type")
        public String grantType = "password";

        @SerializedName("username")
        public String username;

        @SerializedName("password")
        public String password;

        @SerializedName("scope")
        public String scope = "";

        @SerializedName("client_id")
        public String clientId;

        @SerializedName("client_secret")
        public String clientSecret;

        public AuthRequest(String username, String password, String clientId, String clientSecret) {
            this.username = username;
            this.password = password;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }

    public static class AuthResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("token_type")
        public String tokenType;

        @SerializedName("expires_in")
        public long expiresIn; // Add if API provides this

        public String getFullToken() {
            return tokenType + " " + accessToken;
        }
    }

    // ===== SUMMARY/DASHBOARD MODELS =====
    public static class DashboardSummary {
        @SerializedName("totalAlerts")
        public int totalAlerts;

        @SerializedName("confirmedThefts")
        public int confirmedThefts;

        @SerializedName("pendingActions")
        public int pendingActions;

        @SerializedName("resolvedThefts")
        public int resolvedThefts;

        @SerializedName("falseAlerts")
        public int falseAlerts;

        @SerializedName("totalFuelLoss")
        public double totalFuelLoss;

        @SerializedName("totalMonetaryLoss")
        public double totalMonetaryLoss;

        @SerializedName("fromDate")
        public String fromDate;

        @SerializedName("toDate")
        public String toDate;
    }
}
