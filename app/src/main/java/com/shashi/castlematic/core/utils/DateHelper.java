package com.shashi.castlematic.core.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static final SimpleDateFormat API_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Get yesterday's date for API (default)
    public static String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return API_FORMAT.format(cal.getTime());
    }

    // Get today's date for API
    public static String getToday() {
        return API_FORMAT.format(new Date());
    }

    // Format date for display (dd/MM/yyyy)
    public static String formatForDisplay(String apiDate) {
        try {
            Date date = API_FORMAT.parse(apiDate);
            return DISPLAY_FORMAT.format(date);
        } catch (Exception e) {
            return apiDate;
        }
    }

    // Format date for API (yyyy-MM-dd)
    public static String formatForApi(String displayDate) {
        try {
            Date date = DISPLAY_FORMAT.parse(displayDate);
            return API_FORMAT.format(date);
        } catch (Exception e) {
            return displayDate;
        }
    }
}
