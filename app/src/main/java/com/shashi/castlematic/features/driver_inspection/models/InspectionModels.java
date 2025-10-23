package com.shashi.castlematic.features.driver_inspection.models;

import java.util.ArrayList;
import java.util.List;

public class InspectionModels {

    // Inspection Type
    public enum InspectionType {
        START_DUTY("Start of Duty", "Pre-Trip Inspection"),
        END_DUTY("End of Duty", "Post-Trip Inspection");

        private final String value;
        private final String displayName;

        InspectionType(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
    }

    // Inspection Item
    public static class InspectionItem {
        public String id;
        public String category;
        public String title;
        public String description;
        public boolean isChecked;
        public String photoPath;
        public String remarks;

        public InspectionItem(String id, String category, String title, String description) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.description = description;
            this.isChecked = false;
            this.photoPath = null;
            this.remarks = "";
        }
    }

    // Complete Inspection Record
    public static class InspectionRecord {
        public String inspectionId;
        public String driverId;
        public String driverName;
        public String vehicleNumber;
        public String inspectionType; // "start" or "end"
        public String timestamp;
        public int odometerReading;
        public double engineHours;  // NEW
        public List<InspectionItem> items;
        public String overallRemarks;

        public InspectionRecord() {
            this.items = new ArrayList<>();
        }
    }


    // Predefined Inspection Checklist
    public static List<InspectionItem> getDefaultInspectionItems() {
        List<InspectionItem> items = new ArrayList<>();

        // Engine & Mechanical
        items.add(new InspectionItem("eng_oil", "Engine & Mechanical", "Engine Oil Level", "Check oil level dipstick"));
        items.add(new InspectionItem("eng_coolant", "Engine & Mechanical", "Coolant Level", "Check coolant reservoir"));
        items.add(new InspectionItem("eng_hydraulic", "Engine & Mechanical", "Hydraulic Fluid", "Check hydraulic fluid level"));
        items.add(new InspectionItem("eng_battery", "Engine & Mechanical", "Battery Condition", "Check battery terminals and level"));
        items.add(new InspectionItem("eng_air_filter", "Engine & Mechanical", "Air Filter", "Check air filter condition"));
        items.add(new InspectionItem("eng_fuel", "Engine & Mechanical", "Fuel Level", "Check fuel gauge reading"));

        // Tires & Wheels
        items.add(new InspectionItem("tire_pressure", "Tires & Wheels", "Tire Pressure", "Check all tires for proper inflation"));
        items.add(new InspectionItem("tire_wear", "Tires & Wheels", "Tire Wear/Damage", "Inspect tires for cuts, wear, bulges"));
        items.add(new InspectionItem("tire_nuts", "Tires & Wheels", "Wheel Nuts", "Check all wheel nuts are tight"));

        // Lights & Electrical
        items.add(new InspectionItem("light_head", "Lights & Electrical", "Headlights", "Test headlights (high and low beam)"));
        items.add(new InspectionItem("light_tail", "Lights & Electrical", "Tail Lights", "Test tail lights and brake lights"));
        items.add(new InspectionItem("light_turn", "Lights & Electrical", "Turn Signals", "Test left and right indicators"));
        items.add(new InspectionItem("light_dash", "Lights & Electrical", "Dashboard Lights", "Check all warning lights"));

        // Safety Equipment
        items.add(new InspectionItem("safe_seatbelt", "Safety Equipment", "Seat Belt", "Check seat belt condition and function"));
        items.add(new InspectionItem("safe_extinguisher", "Safety Equipment", "Fire Extinguisher", "Check extinguisher present and valid"));
        items.add(new InspectionItem("safe_firstaid", "Safety Equipment", "First Aid Kit", "Verify first aid kit is present"));
        items.add(new InspectionItem("safe_warning", "Safety Equipment", "Warning Triangle", "Check warning triangle/reflectors"));

        // Exterior Condition
        items.add(new InspectionItem("ext_body", "Exterior Condition", "Body Damage", "Inspect for new dents, scratches, damage"));
        items.add(new InspectionItem("ext_mirrors", "Exterior Condition", "Mirrors", "Check mirrors are intact and adjustable"));
        items.add(new InspectionItem("ext_windshield", "Exterior Condition", "Windshield", "Check for cracks or chips"));
        items.add(new InspectionItem("ext_horn", "Exterior Condition", "Horn", "Test horn functionality"));

        // Cab Interior
        items.add(new InspectionItem("cab_instruments", "Cab Interior", "Instruments", "Check all gauges and displays working"));
        items.add(new InspectionItem("cab_brakes", "Cab Interior", "Brakes", "Test brake pedal feel and response"));
        items.add(new InspectionItem("cab_steering", "Cab Interior", "Steering", "Check steering for play or stiffness"));
        items.add(new InspectionItem("cab_parking", "Cab Interior", "Parking Brake", "Test parking brake holds"));

        return items;
    }
}
