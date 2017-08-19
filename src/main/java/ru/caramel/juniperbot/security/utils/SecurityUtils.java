package ru.caramel.juniperbot.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;

public class SecurityUtils {

    private SecurityUtils() {
        // private
    }

    public static DiscordUserDetails getCurrentUser() {
        Authentication userAuth = getUserAuthentication();
        if (userAuth != null && userAuth.getDetails() instanceof DiscordUserDetails) {
            return (DiscordUserDetails) userAuth.getDetails();
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
            return  (OAuth2Authentication) auth;
        }
        return null;
    }
}
