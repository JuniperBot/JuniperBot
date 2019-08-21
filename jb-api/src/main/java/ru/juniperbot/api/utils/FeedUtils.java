/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.utils;

import com.rometools.rome.feed.synd.SyndEntry;
import org.apache.commons.collections4.CollectionUtils;
import org.jdom2.Element;

import java.util.List;

public class FeedUtils {

    private FeedUtils() {
        // private class
    }

    public static String getForeignValue(SyndEntry entry, String name) {
        if (entry == null) {
            return null;
        }
        Element element = getForeignValue(entry.getForeignMarkup(), name);
        return element != null ? element.getValue() : null;
    }

    public static Element getForeignValue(List<Element> elements, String name) {
        if (CollectionUtils.isEmpty(elements)) {
            return null;
        }
        return elements.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
