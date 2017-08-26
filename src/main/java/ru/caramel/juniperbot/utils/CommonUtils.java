package ru.caramel.juniperbot.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.Objects;
import java.util.stream.Stream;

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
}
