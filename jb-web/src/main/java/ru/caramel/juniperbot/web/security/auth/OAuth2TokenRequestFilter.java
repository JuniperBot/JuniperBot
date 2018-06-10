package ru.caramel.juniperbot.web.security.auth;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import ru.caramel.juniperbot.core.utils.GsonUtils;
import ru.caramel.juniperbot.web.dto.TokenRequestDto;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OAuth2TokenRequestFilter extends AbstractAuthenticationProcessingFilter {

    private final static Gson gson = GsonUtils.create();

    @Getter
    @Setter
    private OAuth2RestTemplate restTemplate;

    @Getter
    @Setter
    private ResourceServerTokenServices tokenServices;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();

    protected OAuth2TokenRequestFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
        setAuthenticationManager(new NoopAuthenticationManager());
        setAuthenticationDetailsSource(authenticationDetailsSource);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        TokenRequestDto requestDto = gson.fromJson(request.getReader(), TokenRequestDto.class);
        if (requestDto.getCode() != null) {
            AccessTokenRequest tokenRequest = restTemplate.getOAuth2ClientContext().getAccessTokenRequest();
            tokenRequest.setCurrentUri(requestDto.getRedirectUri());
            tokenRequest.setAuthorizationCode(requestDto.getCode());
        }

        OAuth2AccessToken accessToken;
        try {
            accessToken = restTemplate.getAccessToken();
        } catch (OAuth2Exception e) {
            throw new BadCredentialsException("Could not obtain access token", e);
        }

        try {
            OAuth2Authentication result = tokenServices.loadAuthentication(accessToken.getValue());
            if (authenticationDetailsSource!=null) {
                request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, accessToken.getValue());
                request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE, accessToken.getTokenType());
                result.setDetails(authenticationDetailsSource.buildDetails(request));
            }
            return result;
        } catch (InvalidTokenException e) {
            throw new BadCredentialsException("Could not obtain user details from token", e);
        }
    }

    private static class NoopAuthenticationManager implements AuthenticationManager {

        @Override
        public Authentication authenticate(Authentication authentication)
                throws AuthenticationException {
            throw new UnsupportedOperationException("No authentication should be done with this AuthenticationManager");
        }
    }
}
