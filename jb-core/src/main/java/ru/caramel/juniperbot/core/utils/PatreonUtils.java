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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.DeserializationFeature;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import ru.caramel.juniperbot.core.model.PatreonMember;

import java.io.UnsupportedEncodingException;

public class PatreonUtils {

    private static final ResourceConverter converter;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        converter = new ResourceConverter(
                objectMapper,
                PatreonMember.class
        );
        converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);
    }

    private PatreonUtils() {
        // private
    }

    public static PatreonMember parseMember(String content) {
        try {
            JSONAPIDocument<PatreonMember> document = converter.readDocument(
                    content.getBytes("UTF-8"),
                    PatreonMember.class
            );
            return document.get();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
