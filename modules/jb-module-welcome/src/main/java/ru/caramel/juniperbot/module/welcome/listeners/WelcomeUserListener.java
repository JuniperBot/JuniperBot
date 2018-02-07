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
package ru.caramel.juniperbot.module.welcome.listeners;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;
import ru.caramel.juniperbot.module.welcome.persistence.entity.WelcomeMessage;
import ru.caramel.juniperbot.module.welcome.persistence.repository.WelcomeMessageRepository;

@Component
public class WelcomeUserListener extends DiscordEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeUserListener.class);

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private WelcomeMessageRepository welcomeMessageRepository;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        WelcomeMessage message = welcomeMessageRepository.findByGuild(event.getGuild());
        if (message == null || !message.isJoinEnabled() || (!message.isJoinToDM() && message.getJoinChannelId() == null)) {
            return;
        }

        if (message.isJoinToDM() && !event.getJDA().getSelfUser().equals(event.getUser())) {
            User user = event.getUser();
            try {
                user.openPrivateChannel().queue(c -> contextService.withContext(event.getGuild(), () ->
                        send(event, c, message.getJoinMessage(), message.isJoinRichEnabled())));
            } catch (Exception e) {
                LOGGER.error("Could not open private channel for user {}", user, e);
            }
        } else {
            MessageChannel channel = event.getGuild().getTextChannelById(message.getJoinChannelId());
            send(event, channel, message.getJoinMessage(), message.isJoinRichEnabled());
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        WelcomeMessage message = welcomeMessageRepository.findByGuildId(event.getGuild().getIdLong());
        if (message == null || !message.isLeaveEnabled() || message.getLeaveChannelId() == null) {
            return;
        }

        MessageChannel channel = event.getGuild().getTextChannelById(message.getLeaveChannelId());
        send(event, channel, message.getLeaveMessage(), message.isLeaveRichEnabled());
    }

    private void send(GenericGuildMemberEvent event, MessageChannel channel, String message, boolean rich) {
        if (channel == null) {
            return;
        }

        // process message
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("user", event instanceof GuildMemberLeaveEvent
                ? event.getUser().getName() : event.getUser().getAsMention());
        resolver.put("guild", event.getGuild().getName());
        message = placeholderHelper.replacePlaceholders(message, resolver);
        if (message.contains("#")) {
            for (TextChannel textChannel : event.getGuild().getTextChannels()) {
                message = message.replace("#" + textChannel.getName(), textChannel.getAsMention());
            }
        }

        MessageBuilder builder = new MessageBuilder();
        if (rich) {
            Guild guild = event.getGuild();
            EmbedBuilder embedBuilder = messageService.getBaseEmbed();
            embedBuilder.setAuthor(guild.getName(), null, guild.getIconUrl());
            embedBuilder.setDescription(message);
            builder.setEmbed(embedBuilder.build());
        } else {
            builder.append(message);
        }
        messageService.sendMessageSilent(channel::sendMessage, builder.build());
    }
}