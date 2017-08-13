package ru.caramel.juniperbot.web.common;

import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.request.Request;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component("userInfoPreparer")
public class UserInfoPreparer implements ViewPreparer {

    @Override
    public void execute(Request tilesContext, AttributeContext attributeContext) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof OAuth2Authentication) {
            OAuth2Authentication oauth = (OAuth2Authentication) auth;
            Authentication userAuth = oauth.getUserAuthentication();
            if (userAuth != null) {
                attributeContext.putAttribute("userDetails", new Attribute(userAuth.getDetails()));
            }
        }
    }
}
