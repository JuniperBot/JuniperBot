package ru.caramel.juniperbot.security.auth;

import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;

import java.util.Map;

public class DiscordTokenServices extends UserInfoTokenServices {

    public DiscordTokenServices(String userInfoEndpointUrl, String clientId) {
        super(userInfoEndpointUrl, clientId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        OAuth2Authentication auth = super.loadAuthentication(accessToken);
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth.getUserAuthentication();
        Map<String, Object> details = (Map<String, Object>) token.getDetails();
        token.setDetails(DiscordUserDetails.create(details));
        return auth;
    }
}
