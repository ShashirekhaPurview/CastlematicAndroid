package com.shashi.castlematic.core.config;

public class AppConfig {
    // API Configuration - UPDATED to use production endpoints
    public static final String DEV_BASE_URL = "http://192.168.0.224:1051/api/";
    public static final String PROD_BASE_URL = "https://api.analytics.castlematic.com/api/";

    // Auth Configuration - UPDATED to use production
    public static final String AUTH_BASE_URL = "https://api.analytics.castlematic.com";

    // UPDATED client credentials to match your curl
    public static final String CLIENT_ID = "MEIL-5345"; // Fixed from MEIL-5345
    public static final String CLIENT_SECRET = "YOUR_ACTUAL_CLIENT_SECRET_HERE"; // Replace with real secret from curl

    // SWITCH to production for now (change back to DEV_BASE_URL if you need local)
    public static final String BASE_URL = PROD_BASE_URL;

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

    // Environment flag for easy switching
    public static final boolean USE_PRODUCTION = true; // Set to true to use production API

    // Get current base URL based on environment
    public static String getCurrentBaseUrl() {
        return USE_PRODUCTION ? PROD_BASE_URL : DEV_BASE_URL;
    }

    // Get current auth URL based on environment
    public static String getCurrentAuthUrl() {
        return USE_PRODUCTION ? AUTH_BASE_URL : "http://192.168.0.224:1051/";
    }
}
