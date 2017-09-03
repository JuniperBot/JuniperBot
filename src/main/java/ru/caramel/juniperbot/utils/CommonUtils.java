package ru.caramel.juniperbot.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;
import java.util.stream.Stream;

public final class CommonUtils {

    private final static DateTimeFormatter HOURS_FORMAT = DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter MINUTES_FORMAT = DateTimeFormat.forPattern("mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter SECONDS_FORMAT = DateTimeFormat.forPattern("ss").withZone(DateTimeZone.UTC);

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

    public static <T> T coalesce(T... objects) {
        return Stream.of(objects).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static String trimTo(String content, int length) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        if (content.length() > length) {
            content = content.substring(0, length - 3) + "...";
        }
        return content;
    }

    public static String mdLink(String title, String url) {
        return String.format("[%s](%s)", StringUtils.isEmpty(title) ? url : title, url);
    }

    public static String getVolumeIcon(int volume) {
        if (volume > 66) {
            return ":loud_sound:";
        } else if (volume > 33) {
            return ":sound:";
        } else if (volume > 0) {
            return ":speaker:";
        }
        return ":mute:";
    }

    public static Long parseMillis(String string) {
        if (StringUtils.isNotEmpty(string)) {
            if (string.matches("^[0-2]?[0-3]:[0-5][0-9]:[0-5][0-9]$")) {
                return HOURS_FORMAT.parseMillis(string);
            } else if (string.matches("^[0-5]?[0-9]:[0-5][0-9]$")) {
                return MINUTES_FORMAT.parseMillis(string);
            } else if (string.matches("^[0-5]?[0-9]$")) {
                return SECONDS_FORMAT.parseMillis(string);
            }
        }
        return null;
    }
}
