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
package ru.caramel.juniperbot.core.patreon.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import ru.caramel.juniperbot.core.patreon.resources.shared.BaseResource;
import ru.caramel.juniperbot.core.patreon.resources.shared.Field;
import ru.caramel.juniperbot.core.patreon.resources.shared.SocialConnections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@Type("user")
public class User extends BaseResource {

    public enum UserField implements Field {

        EMAIL("email", true),
        FIRST_NAME("first_name", true),
        LAST_NAME("last_name", true),
        FULL_NAME("full_name", true),
        IS_EMAIL_VERIFIED("is_email_verified", true),
        VANITY("vanity", true),
        ABOUT("about", true),
        IMAGE_URL("image_url", true),
        THUMB_URL("thumb_url", true),
        CAN_SEE_NSFW("can_see_nsfw", true),
        CREATED("created", true),
        URL("url", true),
        LIKE_COUNT("like_count", true),
        HIDE_PLEDGES("hide_pledges", true),
        SOCIAL_CONNECTIONS("social_connections", true),
        ;

        /**
         * The field's name from the API in JSON
         */
        @Getter
        public final String propertyName;

        /**
         * Whether the field is included by default
         */
        @Getter
        public final boolean isDefault;

        UserField(String propertyName, boolean isDefault) {
            this.propertyName = propertyName;
            this.isDefault = isDefault;
        }

        public static Collection<UserField> getDefaultFields() {
            return Arrays.stream(values()).filter(UserField::isDefault).collect(Collectors.toList());
        }
    }

    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private boolean emailVerified;
    private String vanity;
    private String about;
    private String imageUrl;
    private String thumbUrl;
    private boolean canSeeNsfw;
    private Date created;
    private String url;
    private Integer likeCount;
    private boolean hidePledges;
    private SocialConnections socialConnections;

    @JsonCreator
    public User(
            @JsonProperty("email") String email,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("is_email_verified") boolean emailVerified,
            @JsonProperty("vanity") String vanity,
            @JsonProperty("about") String about,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("thumb_url") String thumbUrl,
            @JsonProperty("can_see_nsfw") boolean canSeeNsfw,
            @JsonProperty("created") Date created,
            @JsonProperty("url") String url,
            @JsonProperty("like_count") Integer likeCount,
            @JsonProperty("hide_pledges") boolean hidePledges,
            @JsonProperty("social_connections") SocialConnections socialConnections
    ) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.emailVerified = emailVerified;
        this.vanity = vanity;
        this.about = about;
        this.imageUrl = imageUrl;
        this.thumbUrl = thumbUrl;
        this.canSeeNsfw = canSeeNsfw;
        this.created = created;
        this.url = url;
        this.likeCount = likeCount;
        this.hidePledges = hidePledges;
        this.socialConnections = socialConnections;
    }
}
