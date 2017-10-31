/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.util.TextUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CommonUtils {

    private final static DateTimeFormatter HOURS_FORMAT = DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter MINUTES_FORMAT = DateTimeFormat.forPattern("mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter SECONDS_FORMAT = DateTimeFormat.forPattern("ss").withZone(DateTimeZone.UTC);


    private final static Pattern VK_LINK_TAG = Pattern.compile("\\[([0-9a-zA-Z_\\.]+)\\|([^\\|\\[\\]]+)\\]");

    private final static Pattern VK_HASH_TAG = Pattern.compile("(#[0-9a-zA-Zа-яА-Я_#]+)");

    private final static Map<String, String> TRANSLIT_MAP = makeTranslitMap();

    private static Map<String, String> makeTranslitMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "а");
        map.put("b", "б");
        map.put("v", "в");
        map.put("g", "г");
        map.put("d", "д");
        map.put("e", "е");
        map.put("yo", "ё");
        map.put("zh", "ж");
        map.put("z", "з");
        map.put("i", "и");
        map.put("j", "й");
        map.put("k", "к");
        map.put("l", "л");
        map.put("m", "м");
        map.put("n", "н");
        map.put("o", "о");
        map.put("p", "п");
        map.put("r", "р");
        map.put("s", "с");
        map.put("t", "т");
        map.put("u", "у");
        map.put("f", "ф");
        map.put("h", "х");
        map.put("ts", "ц");
        map.put("ch", "ч");
        map.put("sh", "ш");
        map.put("`", "ъ");
        map.put("y", "у");
        map.put("'", "ь");
        map.put("yu", "ю");
        map.put("ya", "я");
        map.put("x", "кс");
        map.put("w", "в");
        map.put("q", "к");
        map.put("iy", "ий");
        return map;
    }

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

    public static String parseVkLinks(String string) {
        return parseVkLinks(string, false);
    }

    public static String parseVkLinks(String string, boolean noLink) {
        if (StringUtils.isEmpty(string)) return string;
        Matcher m = VK_LINK_TAG.matcher(string);
        StringBuffer sb = new StringBuffer(string.length());
        while (m.find()) {
            m.appendReplacement(sb, noLink ? m.group(2)
                    : String.format("[%s](https://vk.com/%s)", m.group(2), m.group(1)));
        }
        m.appendTail(sb);

        string = sb.toString();

        if (!noLink) {
            try {
                m = VK_HASH_TAG.matcher(string);
                sb = new StringBuffer(string.length());
                while (m.find()) {
                    m.appendReplacement(sb, noLink ? m.group(2)
                            : String.format("[%s](https://vk.com/feed?section=search&q=%s)", m.group(1), UriUtils.encode(m.group(1), "UTF-8")));
                }
                m.appendTail(sb);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            string = sb.toString();
        }
        return maskDiscordFormat(string);
    }

    public static String maskDiscordFormat(String string) {
        if (StringUtils.isEmpty(string)) return string;
        return string
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~~", "\\~\\~");
    }

    public static String makeLink(String title, String url) {
        return String.format("[%s](%s)", title, url);
    }

    public static String untranslit(String text) {
        Function<String, String> get = s -> {
            String result = TRANSLIT_MAP.get(s.toLowerCase());
            return result == null ? "" : (Character.isUpperCase(s.charAt(0)) ? (result.charAt(0) + "").toUpperCase() +
                    (result.length() > 1 ? result.substring(1) : "") : result);
        };

        int len = text.length();
        if (len == 0) {
            return text;
        }
        if (len == 1) {
            return get.apply(text);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; ) {
            // get next 2 symbols
            String toTranslate = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
            // trying to translate
            String translated = get.apply(toTranslate);
            // if these 2 symbols are not connected try to translate one by one
            if (TextUtils.isEmpty(translated)) {
                translated = get.apply(toTranslate.charAt(0) + "");
                sb.append(TextUtils.isEmpty(translated) ? toTranslate.charAt(0) : translated);
                i++;
            } else {
                sb.append(TextUtils.isEmpty(translated) ? toTranslate : translated);
                i += 2;
            }
        }
        return sb.toString();
    }
}
