package com.shashi.castlematic.core.config;

public class AppConfig {
    // API Configuration - Updated with working endpoints
    public static final String DEV_BASE_URL = "http://192.168.0.224:1051/api/";
    public static final String PROD_BASE_URL = "https://api.meil.in/api/";

    // Auth Configuration - Use local endpoint that's actually working
    public static final String AUTH_BASE_URL = "http://192.168.0.224:1051/"; // Changed from ngrok

    public static final String CLIENT_ID = "MEIL-5345"; // Updated to match your curl
    public static final String CLIENT_SECRET = "string"; // Updated to match your curl (replace with real value)

    // Use dev for now
    public static final String BASE_URL = DEV_BASE_URL;

    // App Settings
    public static final double DEFAULT_FUEL_COST = 90.0;
    public static final int DEFAULT_THRESHOLD = 10;
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";

    // Auth Endpoints
    public static final String AUTH_TOKEN_ENDPOINT = "auth/token";

    // Fuel Theft Endpoints
    public static final String FUEL_THEFT = "fuel-theft";
    public static final String FUEL_THEFT_LOGS = "fuel-theft-logs";
    public static final String FUEL_THEFT_LOG = "fuel-theft-log";
    public static final String FUEL_THEFT_RESOLVE = "fuel-theft-log/resolve";
    public static final String VEHICLE_COUNTS = "vehicle-idling-hours";

    // Test credentials
    public static final String TEST_USERNAME = "USER";
    public static final String TEST_PASSWORD = "purview@2025";
}
