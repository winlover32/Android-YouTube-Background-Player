package com.smedic.tubtub.utils;

/**
 * Created by smedic on 4.2.16..
 */
public class Utils {

    private static final String TAG = "SMEDIC JSON";

    /* Converting ISO8601 formatted duration to normal readable time */
    public static String convertISO8601DurationToNormalTime(String isoTime) {
        String formattedTime = new String();

        if (isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String minutes = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("M"));
            String seconds = isoTime.substring(isoTime.indexOf("M") + 1, isoTime.indexOf("S"));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("M"));
            String seconds = isoTime.substring(isoTime.indexOf("M") + 1, isoTime.indexOf("S"));
            formattedTime = minutes + ":" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String seconds = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("S"));
            formattedTime = hours + ":00:" + formatTo2Digits(seconds);
        } else if (isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            String minutes = isoTime.substring(isoTime.indexOf("H") + 1, isoTime.indexOf("M"));
            formattedTime = hours + ":" + formatTo2Digits(minutes) + ":00";
        } else if (!isoTime.contains("H") && !isoTime.contains("M") && isoTime.contains("S")) {
            String seconds = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("S"));
            formattedTime = "0:" + formatTo2Digits(seconds);
        } else if (!isoTime.contains("H") && isoTime.contains("M") && !isoTime.contains("S")) {
            String minutes = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("M"));
            formattedTime = minutes + ":00";
        } else if (isoTime.contains("H") && !isoTime.contains("M") && !isoTime.contains("S")) {
            String hours = isoTime.substring(isoTime.indexOf("T") + 1, isoTime.indexOf("H"));
            formattedTime = hours + ":00:00";
        }

        return formattedTime;
    }

    private static String formatTo2Digits(String str) {
        if (str.length() < 2) {
            str = "0" + str;
        }
        return str;
    }

}
