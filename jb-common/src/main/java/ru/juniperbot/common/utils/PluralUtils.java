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
package ru.juniperbot.common.utils;

import java.util.*;
import java.util.regex.Pattern;

public final class PluralUtils {

    private static final Map<String/* lang */, Map<String/* key */, Pattern>> pluralRules;

    static {
        Map<String, Pattern> ruRules = new LinkedHashMap<>();
        ruRules.put("zero", Pattern.compile("^\\d*0$"));
        ruRules.put("one", Pattern.compile("^(-?\\d*[^1])?1$"));
        ruRules.put("two", Pattern.compile("^(-?\\d*[^1])?2$"));
        ruRules.put("few", Pattern.compile("(^(-?\\d*[^1])?3)|(^(-?\\d*[^1])?4)$"));
        ruRules.put("many", Pattern.compile("^\\d+$"));
        ruRules = Collections.unmodifiableMap(ruRules);

        Map<String, Pattern> enRules = new LinkedHashMap<>();
        enRules.put("zero", Pattern.compile("^0$"));
        enRules.put("one", Pattern.compile("^1$"));
        enRules.put("other", Pattern.compile("^\\d+$"));
        enRules = Collections.unmodifiableMap(enRules);

        Map<String/* lang */, Map<String/* key */, Pattern>> rules = new HashMap<>();
        rules.put(LocaleUtils.RU_LOCALE, ruRules);
        rules.put(Locale.ENGLISH.getLanguage(), enRules);

        pluralRules = Collections.unmodifiableMap(rules);
    }

    private PluralUtils() {
        // helper class
    }

    public static String getPluralKey(Locale locale, long value) {
        String str = String.valueOf(value);
        String key = null;

        Map<String/* key */, Pattern> rules = locale != null ? pluralRules.get(locale.getLanguage()) : null;
        if (rules == null) {
            rules = pluralRules.get(Locale.ENGLISH.getLanguage());
        }

        for (Map.Entry<String, Pattern> plural : rules.entrySet()) {
            if (plural.getValue().matcher(str).find()) {
                key = plural.getKey();
                break;
            }
        }
        if (key == null) {
            key = "other";
        }
        return key;
    }
}
