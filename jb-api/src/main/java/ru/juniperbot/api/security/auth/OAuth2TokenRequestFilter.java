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
package ru.juniperbot.api.security.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import ru.juniperbot.api.security.model.TokenRequestDto;
import ru.juniperbot.common.utils.GsonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OAuth2TokenRequestFilter extends AbstractAuthenticationProcessingFilter {

    private final static Gson gson = GsonUtils.create();

    @Autowired
    private OAuth2ProtectedResourceDetails resource;

    @Autowired
    private AuthorizationCodeAccessTokenProvider tokenProvider;

    @Getter
    @Setter
    private ResourceServerTokenServices tokenServices;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();

    private LoadingCache<TokenRequestDto, OAuth2AccessToken> tokenCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public OAuth2AccessToken load(TokenRequestDto requestDto) {
                            OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource);
                            restTemplate.setAccessTokenProvider(tokenProvider);
                            if (requestDto.getCode() != null) {
                                AccessTokenRequest tokenRequest = restTemplate.getOAuth2ClientContext().getAccessTokenRequest();
                                tokenRequest.setCurrentUri(requestDto.getRedirectUri());
                                tokenRequest.setAuthorizationCode(requestDto.getCode());
                            }
                            try {
                                return restTemplate.getAccessToken();
                            } catch (OAuth2Exception e) {
                                throw new BadCredentialsException("Could not obtain access token", e);
                            }
                        }
                    });

    protected OAuth2TokenRequestFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
        setAuthenticationManager(new NoopAuthenticationManager());
        setAuthenticationDetailsSource(authenticationDetailsSource);
    }

    @Override
    @Synchronized
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        TokenRequestDto requestDto = gson.fromJson(request.getReader(), TokenRequestDto.class);
        OAuth2AccessToken accessToken;
        try {
            accessToken = tokenCache.get(requestDto);
        } catch (ExecutionException e) {
            throw new BadCredentialsException("Could not obtain access token", e);
        }

        try {
            OAuth2Authentication result = tokenServices.loadAuthentication(accessToken.getValue());
            if (authenticationDetailsSource != null) {
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
