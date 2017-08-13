package ru.caramel.juniperbot.web.common;

import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.request.Request;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.security.utils.SecurityUtils;

@Component("userInfoPreparer")
public class UserInfoPreparer implements ViewPreparer {

    @Override
    public void execute(Request tilesContext, AttributeContext attributeContext) {
        DiscordUserDetails details = SecurityUtils.getCurrentUser();
        if (details != null) {
            attributeContext.putAttribute("userDetails", new Attribute(details));
        }
    }
}
