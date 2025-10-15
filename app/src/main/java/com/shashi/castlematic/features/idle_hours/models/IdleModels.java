package com.shashi.castlematic.features.idle_hours.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class IdleModels {

    // Response model for vehicle idling hours
    public static class VehicleIdleResponse {
        @SerializedName("vehicles")
        public List<VehicleIdle> vehicles;
    }

    // Individual vehicle idle data
    public static class VehicleIdle {
        @SerializedName("sno")
        public int sno;

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("vehicleType")
        public String vehicleType;

        @SerializedName("idlingHours")
        public String idlingHours; // Format: "27:53"

        @SerializedName("project")
        public String project;

        @SerializedName("ownedOrHired")
        public String ownedOrHired;

        @SerializedName("status")
        public String status;

        // Track remaining hours locally (calculated from assignments)
        public transient double remainingHours;

        // Helper method to get clean vehicle name
        public String getCleanVehicleName() {
            if (vehicleName == null) return "Unknown";
            return vehicleName.replace("_", " ");
        }

        // Helper to convert "HH:MM" to decimal hours
        public double getIdleHoursDecimal() {
            if (idlingHours == null || idlingHours.isEmpty()) return 0.0;
            try {
                String[] parts = idlingHours.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return hours + (minutes / 60.0);
            } catch (Exception e) {
                return 0.0;
            }
        }

        // Helper to format hours
        public String getFormattedIdleHours() {
            return idlingHours != null ? idlingHours : "0:00";
        }
    }

    // Request model for fetching idle hours
    public static class IdleHoursRequest {
        @SerializedName("fromDate")
        public String fromDate;

        @SerializedName("toDate")
        public String toDate;

        @SerializedName("ownedOrHired")
        public String ownedOrHired; // "Both", "Owned", "Hired"

        public IdleHoursRequest(String fromDate, String toDate, String ownedOrHired) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.ownedOrHired = ownedOrHired;
        }
    }

    // Request model for assigning idle reason
    public static class AssignReasonRequest {
        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("vehicleCategory")
        public String vehicleCategory;

        @SerializedName("idlingHours")
        public String idlingHours;

        @SerializedName("project")
        public String project;

        @SerializedName("ownedOrHired")
        public String ownedOrHired;

        @SerializedName("date")
        public String date;

        @SerializedName("assignedHours")
        public String assignedHours;

        @SerializedName("assignedReason")
        public String assignedReason;

        public AssignReasonRequest(VehicleIdle vehicle, String date, String assignedHours, String assignedReason) {
            this.vehicleName = vehicle.vehicleName;
            this.vehicleCategory = vehicle.vehicleCategory;
            this.idlingHours = vehicle.idlingHours;
            this.project = vehicle.project;
            this.ownedOrHired = vehicle.ownedOrHired;
            this.date = date;
            this.assignedHours = assignedHours;
            this.assignedReason = assignedReason;
        }
    }

    // Idle reasons enum
    // Update the IdleReason enum to only 4 options
    public enum IdleReason {
        NO_WORK_FRONT("No Work Front"),
        OPERATOR_NOT_AVAILABLE("Operator Not Available"),
        BREAKDOWN("Breakdown"),
        SURPLUS("Surplus");

        private final String displayName;

        IdleReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String[] getAllReasons() {
            IdleReason[] reasons = values();
            String[] displayNames = new String[reasons.length];
            for (int i = 0; i < reasons.length; i++) {
                displayNames[i] = reasons[i].displayName;
            }
            return displayNames;
        }
    }

}
