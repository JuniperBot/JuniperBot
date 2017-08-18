package ru.caramel.juniperbot.web.common;

import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.request.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.security.utils.SecurityUtils;

@Component("userInfoPreparer")
public class UserInfoPreparer implements ViewPreparer {

    @Autowired
    private DiscordClient discordClient;

    @Override
    public void execute(Request tilesContext, AttributeContext attributeContext) {
        DiscordUserDetails details = SecurityUtils.getCurrentUser();
        if (details != null) {
            attributeContext.putAttribute("userDetails", new Attribute(details));
        }
        attributeContext.putAttribute("discordConnected", new Attribute(discordClient.isConnected()));
    }
}
