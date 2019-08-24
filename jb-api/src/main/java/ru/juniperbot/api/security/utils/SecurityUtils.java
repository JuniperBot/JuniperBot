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
package ru.juniperbot.api.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import ru.juniperbot.api.security.model.DiscordUserDetails;

public class SecurityUtils {

    private SecurityUtils() {
        // private
    }

    public static DiscordUserDetails getCurrentUser() {
        return getDetails(getUserAuthentication());
    }

    public static DiscordUserDetails getDetails(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof DiscordUserDetails) {
            return (DiscordUserDetails) authentication.getDetails();
        }
        return null;
    }

    public static String getToken() {
        OAuth2AuthenticationDetails details = getTokenDetails();
        return details != null ? details.getTokenValue() : null;
    }

    public static OAuth2AuthenticationDetails getTokenDetails() {
        OAuth2Authentication auth = getTokenAuthentication();
        if (auth != null && auth.getDetails() instanceof OAuth2AuthenticationDetails) {
            return (OAuth2AuthenticationDetails) auth.getDetails();
        }
        return null;
    }

    public static Authentication getUserAuthentication() {
        OAuth2Authentication auth = getTokenAuthentication();
        return auth != null ? auth.getUserAuthentication() : null;
    }

    public static OAuth2Authentication getTokenAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof OAuth2Authentication) {
            return (OAuth2Authentication) auth;
        }
        return null;
    }

    public static boolean isAuthenticated() {
        Authentication userAuth = getUserAuthentication();
        return userAuth != null && userAuth.isAuthenticated();
    }
}
