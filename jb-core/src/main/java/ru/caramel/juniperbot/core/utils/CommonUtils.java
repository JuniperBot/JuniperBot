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
package ru.caramel.juniperbot.core.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.util.UriUtils;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CommonUtils {

    public final static String EMPTY_SYMBOL = "\u2800";

    private final static DateTimeFormatter HOURS_FORMAT = DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter MINUTES_FORMAT = DateTimeFormat.forPattern("mm:ss").withZone(DateTimeZone.UTC);

    private final static DateTimeFormatter SECONDS_FORMAT = DateTimeFormat.forPattern("ss").withZone(DateTimeZone.UTC);

    private final static Pattern VK_LINK_TAG = Pattern.compile("\\[([0-9a-zA-Z_\\.]+)\\|([^\\|\\[\\]]+)\\]");

    private final static Pattern VK_HASH_TAG = Pattern.compile("(#[0-9a-zA-Zа-яА-Я_#@]+)");

    private static final Pattern CODE_PATTERN = Pattern.compile("\\s*```(groovy\\s+)?((.|\\n)+)```\\s*", Pattern.MULTILINE);

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

    public static String trimTo(String content, int minLength, int maxLength) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        if (content.length() > maxLength) {
            content = content.substring(0, maxLength - 3) + "...";
        }
        if (content.length() < minLength) {
            StringBuilder result = new StringBuilder(content);
            while (result.length() < minLength) {
                result.append("_");
            }
            content = result.toString();
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

    public static String getHTMLAwtColor(Color color) {
        if (color == null) {
            return "auto";
        }
        String red = Integer.toHexString(color.getRed());
        String green = Integer.toHexString(color.getGreen());
        String blue = Integer.toHexString(color.getBlue());

        return "#" +
                (red.length() == 1 ? "0" + red : red) +
                (green.length() == 1 ? "0" + green : green) +
                (blue.length() == 1 ? "0" + blue : blue);
    }

    public static String unwrapCode(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        Matcher m = CODE_PATTERN.matcher(value);
        if (m.find()) {
            return m.group(2);
        }
        return value;
    }

    public static String formatUser(User user) {
        return String.format("%s#%s", user.getName(), user.getDiscriminator());
    }

    public static DateTime getDate(OffsetDateTime offsetDateTime) {
        return new DateTime(offsetDateTime.toEpochSecond() * 1000).withZone(DateTimeZone.UTC);
    }

    public static <T> List<T> reverse(List<T> collection, List<T> part) {
        List<T> arrayList = new ArrayList<>(collection);
        if (CollectionUtils.isNotEmpty(part)) {
            arrayList.removeAll(part);
        }
        return arrayList;
    }

    /**
     * @param colorStr e.g. "FFFFFF"
     * @return
     */
    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf(colorStr.substring(0, 2), 16),
                Integer.valueOf(colorStr.substring(2, 4), 16),
                Integer.valueOf(colorStr.substring(4, 6), 16));
    }

    public static Role getHighestRole(Member member) {
        if (member == null || CollectionUtils.isEmpty(member.getRoles())) {
            return null;
        }
        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.sort(Comparator.comparingInt(Role::getPosition));
        return roles.get(0);
    }

    public static String getUrl(String url) {
        if (StringUtils.isEmpty(url) || url.length() > MessageEmbed.URL_MAX_LENGTH) {
            return null;
        }
        if (EmbedBuilder.URL_PATTERN.matcher(url).matches()) {
            return url;
        }
        try {
            String result = java.net.URLDecoder.decode(url, "UTF-8");
            if (EmbedBuilder.URL_PATTERN.matcher(result).matches()) {
                return result;
            }
        } catch (Exception e) {
            // nah I don't care
        }
        return null;
    }

    public static <T extends Enum<T>> List<T> safeEnumSet(Collection<?> collection, Class<T> type) {
        return Stream.of(type.getEnumConstants())
                .filter(e -> collection.contains(e.name()))
                .collect(Collectors.toList());
    }
}
