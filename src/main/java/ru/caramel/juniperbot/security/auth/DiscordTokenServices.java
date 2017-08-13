package ru.caramel.juniperbot.security.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.FixedAuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.FixedPrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import ru.caramel.juniperbot.security.model.DiscordGuildDetails;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.security.utils.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class DiscordTokenServices implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordTokenServices.class);

    private final String userInfoEndpointUrl;

    private final String guildsInfoEndpointUrl;

    private final String clientId;

    private final OAuth2ProtectedResourceDetails resource;

    private Map<String, OAuth2RestTemplate> restTemplates = new ConcurrentHashMap<>();

    @Setter
    @Getter
    private DiscordHttpRequestFactory requestFactory;

    @Setter
    @Getter
    private AuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

    @Setter
    @Getter
    private PrincipalExtractor principalExtractor = new FixedPrincipalExtractor();

    private LoadingCache<String, List<DiscordGuildDetails>> guilds = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, List<DiscordGuildDetails>>() {
                        public List<DiscordGuildDetails> load(String accessToken) {
                            List<Map<Object, Object>> list = executeRequest(List.class, guildsInfoEndpointUrl, accessToken);
                            return list.stream().map(DiscordGuildDetails::create).collect(Collectors.toList());
                        }
                    });

    public DiscordTokenServices(String userInfoEndpointUrl, String guildsInfoEndpointUrl, String clientId, OAuth2ProtectedResourceDetails resource) {
        this.userInfoEndpointUrl = userInfoEndpointUrl;
        this.guildsInfoEndpointUrl = guildsInfoEndpointUrl;
        this.clientId = clientId;
        this.resource = resource;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        Map map = executeRequest(Map.class, userInfoEndpointUrl, accessToken);
        Object principal = principalExtractor.extractPrincipal(map);
        principal = (principal == null ? "unknown" : principal);
        List<GrantedAuthority> authorities = authoritiesExtractor.extractAuthorities(map);
        OAuth2Request request = new OAuth2Request(null, clientId, null, true, null,
                null, null, null, null);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, "N/A", authorities);
        token.setDetails(DiscordUserDetails.create(map));
        return new OAuth2Authentication(request, token);
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
        return getCurrentGuilds().stream().filter(e -> !editable || (editable && (e.isOwner()
                || e.getPermissions().contains(Permission.ADMINISTRATOR)))).collect(Collectors.toList());
    }

    @SuppressWarnings({ "unchecked" })
    private <T> T executeRequest(Class<T> clazz, String path, String accessToken) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting user info from: " + path);
        }
        return restTemplates.computeIfAbsent(accessToken, e -> {
            OAuth2RestTemplate newTemplate = new OAuth2RestTemplate(resource);
            newTemplate.setRequestFactory(requestFactory);
            DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(accessToken);
            token.setTokenType(DefaultOAuth2AccessToken.BEARER_TYPE);
            newTemplate.getOAuth2ClientContext().setAccessToken(token);
            return newTemplate;
        }).getForEntity(path, clazz).getBody();
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }
}
