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
package ru.caramel.juniperbot.module.social.commands;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.command.model.AbstractCommand;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.core.common.model.exception.ValidationException;
import ru.caramel.juniperbot.module.social.model.InstagramMedia;
import ru.caramel.juniperbot.module.social.model.InstagramProfile;
import ru.caramel.juniperbot.module.social.service.impl.InstagramService;
import ru.caramel.juniperbot.module.social.service.impl.PostService;

import java.util.List;

@DiscordCommand(key = "discord.command.post.key",
        description = "discord.command.post.desc",
        group = "discord.command.group.fun",
        priority = 5)
public class PostCommand extends AbstractCommand {

    @Autowired
    private InstagramService instagramService;

    @Autowired
    protected PostService postService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int count = parseCount(content);
        InstagramProfile profile = instagramService.getRecent();

        if (profile == null) {
            messageService.onError(message.getChannel(), "discord.command.post.error");
            return false;
        }
        List<InstagramMedia> medias = profile.getFeed();
        if (medias.isEmpty()) {
            messageService.onMessage(message.getChannel(), "discord.command.post.empty");
            return false;
        }

        if (count > medias.size()) {
            messageService.onMessage(message.getChannel(), "discord.command.post.exceed", medias.size());
            count = medias.size();
        }
        medias = medias.subList(0, count);
        postService.post(profile, medias, message.getChannel());
        return true;
    }

    private static int parseCount(String content) throws ValidationException {
        int count = 1;
        if (StringUtils.isNotEmpty(content)) {
            try {
                count = Integer.parseInt(content);
            } catch (NumberFormatException e) {
                throw new ValidationException("discord.global.integer.parseError");
            }
            if (count == 0) {
                throw new ValidationException("discord.command.post.parse.zero");
            } else if (count > PostService.MAX_DETAILED) {
                throw new ValidationException("discord.command.post.parse.max");
            } else if (count < 0) {
                throw new ValidationException("discord.global.integer.negative");
            }
        }
        return count;
    }
}
