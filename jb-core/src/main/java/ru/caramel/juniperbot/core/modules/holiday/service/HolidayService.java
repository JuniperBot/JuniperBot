package ru.caramel.juniperbot.core.modules.holiday.service;

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
import ru.caramel.juniperbot.core.modules.holiday.persistence.entity.NewYearNotification;
import ru.caramel.juniperbot.core.modules.holiday.persistence.repository.NewYearNotificationRepository;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;

@Service
public class HolidayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HolidayService.class);

    @Autowired
    private NewYearNotificationRepository repository;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Scheduled(cron = "0 0 0 1 1 ?") // NEW YEAR (0 0 0 1 1 ?) | EVERY MIN (0 * * ? * *)
    public void execute() {
        if (!discordService.isConnected()) {
            return;
        }
        NewYearNotification notification = repository.findOneByGuildId(null);
        JDA jda = discordService.getJda();
        for (Guild guild : jda.getGuilds()) {
            if (!discordService.isConnected()) {
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
