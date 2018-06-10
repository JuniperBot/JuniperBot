package ru.caramel.juniperbot.web.security.auth;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import ru.caramel.juniperbot.core.utils.GsonUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class TokenRequestSuccessHandler implements AuthenticationSuccessHandler {

    private static final Gson gson = GsonUtils.create();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("access_token", details.getTokenValue());
        responseMap.put("token_type", details.getTokenType());
        String content = gson.toJson(responseMap);
        IOUtils.write(content, response.getOutputStream(), Charset.defaultCharset());
    }
}
