/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
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
