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
package ru.juniperbot.module.misc.model.dogapi;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;
import java.util.stream.Collectors;

@Builder
public class DogSearchQuery {

    public enum Size {
        THUMB("thumb"),
        SMALL("small"),
        MEDIUM("med"),
        FULL("full");

        private String key;

        Size(String key) {
            this.key = key;
        }
    }

    @Getter
    public enum MimeType {
        JPG("jpg"),
        PNG("png"),
        GIF("gif");

        private String key;

        MimeType(String key) {
            this.key = key;
        }
    }

    public enum Format {
        JSON("json"),
        SRC("src");

        private String key;

        Format(String key) {
            this.key = key;
        }
    }

    public enum Order {
        RANDOM("RANDOM"),
        DESC("DESC"),
        ASC("ASC");

        private String key;

        Order(String key) {
            this.key = key;
        }
    }

    private Size size;

    @Singular
    private Set<MimeType> mimeTypes;

    private Format format;

    private Boolean hasBreeds;

    private Order order;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int limit = 0;

    public UriComponentsBuilder toUri() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("images/search");
        if (size != null) {
            builder.queryParam("size", size.key);
        }
        if (CollectionUtils.isNotEmpty(mimeTypes)) {
            builder.queryParam("mime_types", mimeTypes.stream()
                    .map(MimeType::getKey)
                    .collect(Collectors.joining(",")));
        }
        if (format != null) {
            builder.queryParam("format", format.key);
        }
        if (hasBreeds != null) {
            builder.queryParam("has_breeds", hasBreeds.toString());
        }
        if (order != null) {
            builder.queryParam("order", order.key);
        }
        return builder
                .queryParam("page", page)
                .queryParam("limit", limit);
    }
}
