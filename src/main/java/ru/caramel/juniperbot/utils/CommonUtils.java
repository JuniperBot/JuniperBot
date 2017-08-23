package ru.caramel.juniperbot.utils;

import org.apache.commons.lang3.time.DurationFormatUtils;

public final class CommonUtils {

    private CommonUtils() {
        // helper class
    }

    public static String formatDuration(long millis) {
        String format = "mm:ss";
        if (millis > 3600000) {
            format = "HH:mm:ss";
        }
        return DurationFormatUtils.formatDuration(millis, format);
    }
}
