package com.shashi.castlematic.features.utilization.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UtilizationModels {

    // Main response model
    public static class VehicleStatsResponse {
        @SerializedName("hours")
        public StatsData hours;

        @SerializedName("kms")
        public StatsData kms;
    }

    // Stats data (for hours or kms)
    public static class StatsData {
        @SerializedName("overallUtilization")
        public String overallUtilization;

        @SerializedName("overallAvailability")
        public String overallAvailability;

        @SerializedName("utilizationRaw")
        public String utilizationRaw;

        @SerializedName("availabilityRaw")
        public String availabilityRaw;

        @SerializedName("groups")
        public List<VehicleGroup> groups;
    }

    // Vehicle group (owned/hired)
    public static class VehicleGroup {
        @SerializedName("name")
        public String name; // "owned" or "hired"

        @SerializedName("utilization")
        public String utilization;

        @SerializedName("availability")
        public String availability;

        @SerializedName("categories")
        public List<VehicleCategory> categories;
    }

    // Vehicle category (DG SET, MOTOR GRADER, etc.)
    public static class VehicleCategory {
        @SerializedName("categoryName")
        public String categoryName;

        @SerializedName("categoryUtilization")
        public String categoryUtilization;

        @SerializedName("categoryAvailability")
        public String categoryAvailability;

        @SerializedName("vehicles")
        public List<Vehicle> vehicles;
    }

    // Individual vehicle
    public static class Vehicle {
        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("vehicleUtilization")
        public String vehicleUtilization;

        @SerializedName("vehicleAvailability")
        public String vehicleAvailability;

        @SerializedName("utilizationRaw")
        public String utilizationRaw;

        @SerializedName("availabilityRaw")
        public String availabilityRaw;

        // Local fields for assignment
        public transient String assignedShift = "Not Assigned";
        public transient String assignedDriver = "Select Driver";

        // Helper to get clean vehicle name
        public String getCleanVehicleName() {
            if (vehicleName == null) return "Unknown";
            return vehicleName.replace("_", " ");
        }

        // Helper to get utilization percentage
        public double getUtilizationPercent() {
            try {
                return Double.parseDouble(vehicleUtilization);
            } catch (Exception e) {
                return 0.0;
            }
        }

        // Helper to get availability percentage
        public double getAvailabilityPercent() {
            try {
                return Double.parseDouble(vehicleAvailability);
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    // Request model
    public static class StatsRequest {
        @SerializedName("fromDate")
        public String fromDate;

        @SerializedName("toDate")
        public String toDate;

        @SerializedName("ownedOrHired")
        public String ownedOrHired; // "Both", "Owned", "Hired"

        public StatsRequest(String fromDate, String toDate, String ownedOrHired) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.ownedOrHired = ownedOrHired;
        }
    }

    // Shift assignment request (placeholder for when you get the endpoint)
    public static class ShiftAssignmentRequest {
        @SerializedName("vehicleName")
        public String vehicleName;

        @SerializedName("date")
        public String date;

        @SerializedName("shift")
        public String shift; // "Single" or "Double"

        @SerializedName("driver")
        public String driver;

        public ShiftAssignmentRequest(String vehicleName, String date, String shift, String driver) {
            this.vehicleName = vehicleName;
            this.date = date;
            this.shift = shift;
            this.driver = driver;
        }
    }

    // Enums for filters
    public enum MeasurementType {
        HOURS("Hours"),
        KMS("KMs"),
        BOTH("Both");

        private final String displayName;

        MeasurementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ShiftType {
        NOT_ASSIGNED("Not Assigned"),
        SINGLE("Single Shift"),
        DOUBLE("Double Shift");

        private final String displayName;

        ShiftType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String[] getAllShifts() {
            ShiftType[] shifts = values();
            String[] displayNames = new String[shifts.length];
            for (int i = 0; i < shifts.length; i++) {
                displayNames[i] = shifts[i].displayName;
            }
            return displayNames;
        }
    }
}
