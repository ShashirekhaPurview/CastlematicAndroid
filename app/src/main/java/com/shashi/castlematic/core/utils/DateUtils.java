package com.shashi.castlematic.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class DateUtils {

    // Different API formats for different endpoints
    public static final String API_DATE_FORMAT = "yyyy-MM-dd"; // For resolve endpoint
    public static final String MARK_API_FORMAT = "dd/MM/yyyy, hh:mm a"; // For mark alert endpoint

    private static final SimpleDateFormat apiFormatter = new SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault());
    private static final SimpleDateFormat markApiFormatter = new SimpleDateFormat(MARK_API_FORMAT, Locale.getDefault());

    /**
     * Convert date string to resolve API format (YYYY-MM-DD)
     */
    public static String formatForAPI(String inputDate) {
        if (inputDate == null || inputDate.isEmpty()) {
            return getCurrentDateForAPI();
        }

        // Remove time part if present and clean up
        String dateOnly = inputDate.split(" ")[0].trim().replaceAll(",$", "");

        // If already in YYYY-MM-DD format, return as is
        if (dateOnly.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateOnly;
        }

        // Convert DD/MM/YYYY to YYYY-MM-DD
        if (dateOnly.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] parts = dateOnly.split("/");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }

        // Convert DD-MM-YYYY to YYYY-MM-DD
        if (dateOnly.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = dateOnly.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }

        // If conversion fails, return current date
        return getCurrentDateForAPI();
    }

    /**
     * Convert date string to mark alert API format (DD/MM/YYYY, HH:MM AM/PM)
     */
    public static String formatForMarkAPI(String inputDate) {
        if (inputDate == null || inputDate.isEmpty()) {
            return getCurrentDateForMarkAPI();
        }

        try {
            // Remove time part if present and clean up
            String dateOnly = inputDate.split(" ")[0].trim().replaceAll(",$", "");

            // Parse the date and add default time (12:00 PM)
            Calendar cal = Calendar.getInstance();

            if (dateOnly.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // YYYY-MM-DD format
                String[] parts = dateOnly.split("-");
                cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            } else if (dateOnly.matches("\\d{2}/\\d{2}/\\d{4}")) {
                // DD/MM/YYYY format
                String[] parts = dateOnly.split("/");
                cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
            } else if (dateOnly.matches("\\d{2}-\\d{2}-\\d{4}")) {
                // DD-MM-YYYY format
                String[] parts = dateOnly.split("-");
                cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
            }

            // Set default time to 12:00 PM
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            return markApiFormatter.format(cal.getTime());

        } catch (Exception e) {
            // If parsing fails, return current date with time
            return getCurrentDateForMarkAPI();
        }
    }

    /**
     * Get current date in resolve API format (YYYY-MM-DD)
     */
    public static String getCurrentDateForAPI() {
        return apiFormatter.format(new Date());
    }

    /**
     * Get current date in mark API format (DD/MM/YYYY, HH:MM AM/PM)
     */
    public static String getCurrentDateForMarkAPI() {
        return markApiFormatter.format(new Date());
    }

    /**
     * Convert timestamp string to mark API format
     */
    public static String timestampToMarkAPI(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return getCurrentDateForMarkAPI();
        }

        try {
            // If timestamp includes time (e.g., "2025-08-30 14:27:11")
            if (timestamp.contains(" ")) {
                String[] parts = timestamp.split(" ");
                String datePart = parts[0];
                String timePart = parts.length > 1 ? parts[1] : "12:00:00";

                // Parse date
                String[] dateParts = datePart.split("-");
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1; // Calendar month is 0-based
                int day = Integer.parseInt(dateParts[2]);

                // Parse time
                String[] timeParts = timePart.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;

                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, hour, minute, 0);

                return markApiFormatter.format(cal.getTime());
            } else {
                // Just date, add default time
                return formatForMarkAPI(timestamp);
            }

        } catch (Exception e) {
            return getCurrentDateForMarkAPI();
        }
    }

    /**
     * Validate if date string is in API format (YYYY-MM-DD)
     */
    public static boolean isValidAPIFormat(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /**
     * Validate if date string is in mark API format (DD/MM/YYYY, HH:MM AM/PM)
     */
    public static boolean isValidMarkAPIFormat(String date) {
        return date != null && date.matches("\\d{2}/\\d{2}/\\d{4}, \\d{2}:\\d{2} [APap][Mm]");
    }
}
