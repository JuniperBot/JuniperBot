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
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.client.HttpClientErrorException;
import ru.juniperbot.api.ApiProperties;
import ru.juniperbot.api.security.model.DiscordGuildDetails;
import ru.juniperbot.api.security.model.DiscordUserDetails;
import ru.juniperbot.api.security.utils.SecurityUtils;
import ru.juniperbot.common.configuration.CommonProperties;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("unchecked")
public class DiscordTokenServices implements ResourceServerTokenServices {

    private final OAuth2ProtectedResourceDetails resource;

    private Map<String, OAuth2RestTemplate> restTemplates = new ConcurrentHashMap<>();

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private ApiProperties apiProperties;

    @Setter
    @Getter
    private DiscordHttpRequestFactory requestFactory;

    @Setter
    @Getter
    private FixedAuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

    private LoadingCache<String, List<DiscordGuildDetails>> guilds = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public List<DiscordGuildDetails> load(String accessToken) {
                            List<Map<Object, Object>> list = executeRequest(List.class,
                                    apiProperties.getDiscord().getGuildsInfoUri(), accessToken);
                            return list.stream().map(DiscordGuildDetails::create).collect(Collectors.toList());
                        }
                    });

    private LoadingCache<String, OAuth2Authentication> authorities = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public OAuth2Authentication load(String accessToken) {
                            Map map = executeRequest(Map.class,
                                    apiProperties.getDiscord().getUserInfoUri(), accessToken);
                            Object principal = map.get("username");
                            principal = (principal == null ? "unknown" : principal);
                            List<GrantedAuthority> authorities = authoritiesExtractor.extractAuthorities(map);
                            OAuth2Request request = new OAuth2Request(null,
                                    apiProperties.getDiscord().getClientId(), null, true, null,
                                    null, null, null, null);
                            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                                    principal, "N/A", authorities);
                            token.setDetails(DiscordUserDetails.create(map));
                            return new OAuth2Authentication(request, token);
                        }
                    });

    public DiscordTokenServices(OAuth2ProtectedResourceDetails resource) {
        this.resource = resource;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        try {
            return authorities.get(accessToken);
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof OAuth2Exception) {
                throw (OAuth2Exception) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    public List<DiscordGuildDetails> getCurrentGuilds() {
        String accessToken = SecurityUtils.getToken();
        if (accessToken == null) {
            throw new InvalidTokenException("No token in context");
        }
        try {
            return guilds.get(accessToken);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DiscordGuildDetails> getCurrentGuilds(boolean editable) {
        return getCurrentGuilds().stream().filter(e -> !editable || hasPermission(e)).collect(Collectors.toList());
    }

    public boolean hasPermission(DiscordGuildDetails details) {
        DiscordUserDetails user = SecurityUtils.getCurrentUser();
        String superUserId = commonProperties.getDiscord().getSuperUserId();
        return details.isOwner() || details.getPermissions().contains(Permission.ADMINISTRATOR)
                || (StringUtils.isNotEmpty(superUserId) && user != null && Objects.equals(superUserId, user.getId()));
    }

    public boolean hasPermission(long guildId) {
        DiscordGuildDetails details = getGuildById(guildId);
        return details != null && hasPermission(details);
    }

    public DiscordGuildDetails getGuildById(long id) {
        String idStr = String.valueOf(id);
        return getCurrentGuilds(false).stream().filter(e -> Objects.equals(idStr, e.getId())).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <T> T executeRequest(Class<T> clazz, String path, String accessToken) {
        if (log.isDebugEnabled()) {
            log.debug("Getting user info from: " + path);
        }
        try {
            return restTemplates.computeIfAbsent(accessToken, e -> {
                OAuth2RestTemplate newTemplate = new OAuth2RestTemplate(resource);
                newTemplate.setRequestFactory(requestFactory);
                DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(accessToken);
                token.setTokenType(DefaultOAuth2AccessToken.BEARER_TYPE);
                newTemplate.getOAuth2ClientContext().setAccessToken(token);
                return newTemplate;
            }).getForEntity(path, clazz).getBody();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED == e.getStatusCode()) {
                throw new InvalidTokenException("Invalid token, access denied");
            } else {
                throw e;
            }
        }
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }
}
