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
package ru.juniperbot.worker.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.model.InstagramMedia;
import ru.juniperbot.common.model.InstagramProfile;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.model.exception.ValidationException;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;

import java.util.List;

// This thing is totally broken now since Instagram requires full authentication even for looking at someone's page
/*@DiscordCommand(key = "discord.command.post.key",
        description = "discord.command.post.desc",
        group = "discord.command.group.fun",
        priority = 5)*/
public class PostCommand extends AbstractCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        int count = parseCount(content);
        InstagramProfile profile = gatewayService.getInstagramProfile();

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
        post(profile, medias, message.getChannel());
        return true;
    }

    public void post(InstagramProfile profile, List<InstagramMedia> medias, MessageChannel channel) {
        if (medias.size() > 0) {
            for (int i = 0; i < Math.min(3, medias.size()); i++) {
                EmbedBuilder builder = convertToEmbed(profile, medias.get(i));
                messageService.sendMessageSilent(channel::sendMessage, builder.build());
            }
        }
    }

    public EmbedBuilder convertToEmbed(InstagramProfile profile, InstagramMedia media) {
        EmbedBuilder builder = new EmbedBuilder()
                .setImage(media.getImageUrl())
                .setTimestamp(media.getDate().toInstant())
                .setColor(contextService.getColor())
                .setAuthor(profile.getFullName(), null, profile.getImageUrl());

        if (media.getText() != null) {
            String text = media.getText();
            if (StringUtils.isNotEmpty(text)) {
                if (text.length() > MessageEmbed.EMBED_MAX_LENGTH_CLIENT) {
                    text = text.substring(0, MessageEmbed.EMBED_MAX_LENGTH_CLIENT - 1);
                }
                String link = media.getLink();
                if (media.getText().length() > 200) {
                    builder.setTitle(link, link);
                    builder.setDescription(text);
                } else {
                    builder.setTitle(text, link);
                }
            }
        }
        return builder;
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
            } else if (count > 3) {
                throw new ValidationException("discord.command.post.parse.max");
            } else if (count < 0) {
                throw new ValidationException("discord.global.integer.negative");
            }
        }
        return count;
    }
}
