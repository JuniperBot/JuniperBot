package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.persistence.entity.NewYearNotification;
import ru.caramel.juniperbot.persistence.repository.NewYearNotificationRepository;
import ru.caramel.juniperbot.service.HolidayService;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.MapPlaceholderResolver;

@Service
public class HolidayServiceImpl implements HolidayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HolidayServiceImpl.class);

    @Autowired
    private NewYearNotificationRepository repository;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private MessageService messageService;

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Scheduled(cron = "0 0 0 1 1 ?") // NEW YEAR (0 0 0 1 1 ?) | EVERY MIN (0 * * ? * *)
    public void execute() {
        if (!discordClient.isConnected()) {
            return;
        }
        NewYearNotification notification = repository.findOneByGuildId(null);
        JDA jda = discordClient.getJda();
        for (Guild guild : jda.getGuilds()) {
            if (!discordClient.isConnected()) {
                return;
            }
            try {
                notifyNewYear(notification, guild);
            } catch (Exception e) {
                LOGGER.error("Cannot send happy new year to guild {} (ID={})", guild.getName(), guild.getId(), e);
            }
        }
    }

    private void notifyNewYear(NewYearNotification baseNotification, Guild guild) {
        NewYearNotification notification = repository.findOneByGuildId(guild.getId());
        if (notification == null) {
            notification = baseNotification;
        }
        if (notification == null || !notification.isEnabled() || StringUtils.isEmpty(notification.getMessage())) {
            return;
        }
        TextChannel channel = getChannel(notification, guild);
        if (channel == null) {
            return;
        }

        String message = notification.getMessage();
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("name", guild.getName());
        message = placeholderHelper.replacePlaceholders(message, resolver);

        EmbedBuilder builder = messageService.getBaseEmbed();
        if (StringUtils.isNotEmpty(notification.getImageUrl())) {
            builder.setImage(notification.getImageUrl());
        }
        builder.setDescription(message);
        messageService.sendMessageSilent(channel::sendMessage, builder.build());
    }

    private TextChannel getChannel(NewYearNotification notification, Guild guild) {
        Member self = guild.getSelfMember();
        TextChannel channel = null;
        if (notification != null && StringUtils.isNotEmpty(notification.getChannelId())) {
            channel = guild.getTextChannelById(notification.getChannelId());
        }

        if (channel != null && PermissionUtil.checkPermission(channel, self, Permission.MESSAGE_WRITE)) {
            return channel;
        }

        for (TextChannel textChannel : guild.getTextChannels()) {
            if (PermissionUtil.checkPermission(textChannel, self, Permission.MESSAGE_WRITE)) {
                return textChannel;
            }
        }
        return null;
    }
}
