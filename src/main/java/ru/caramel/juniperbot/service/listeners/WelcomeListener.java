package ru.caramel.juniperbot.service.listeners;

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
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;
import ru.caramel.juniperbot.persistence.entity.WelcomeMessage;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.MapPlaceholderResolver;

@Component
public class WelcomeListener extends DiscordEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeListener.class);

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        WelcomeMessage message = configService.getWelcomeMessage(event.getGuild().getIdLong());
        if (message == null || !message.isJoinEnabled() || (!message.isJoinToDM() && message.getJoinChannelId() == null)) {
            return;
        }

        MessageChannel channel = null;
        if (message.isJoinToDM() && !event.getJDA().getSelfUser().equals(event.getUser())) {
            User user = event.getUser();
            try {
                channel = user.openPrivateChannel().complete();
            } catch (Exception e) {
                LOGGER.error("Could not open private channel for user {}", user, e);
            }
        }
        if (channel == null) {
            channel = event.getGuild().getTextChannelById(message.getJoinChannelId());
        }
        send(event, channel, message.getJoinMessage(), message.isJoinRichEnabled());
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        WelcomeMessage message = configService.getWelcomeMessage(event.getGuild().getIdLong());
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
        resolver.put(messageService.getMessage("custom.commands.placeholder.user"), event instanceof GuildMemberLeaveEvent
                ? event.getUser().getName() : event.getUser().getAsMention());
        resolver.put(messageService.getMessage("custom.commands.placeholder.guild"), event.getGuild().getName());
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