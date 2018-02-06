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
package ru.caramel.juniperbot.module.holiday.service;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;
import ru.caramel.juniperbot.module.holiday.persistence.entity.NewYearNotification;
import ru.caramel.juniperbot.module.holiday.persistence.repository.NewYearNotificationRepository;

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
        NewYearNotification notification = repository.findOneByGuildId(null);
        ShardManager jda = discordService.getShardManager();
        for (Guild guild : jda.getGuilds()) {
            if (discordService.isConnected(guild.getIdLong())) {
                try {
                    notifyNewYear(notification, guild);
                } catch (Exception e) {
                    LOGGER.error("Cannot send happy new year to guild {} (ID={})", guild.getName(), guild.getId(), e);
                }
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

        if (channel != null && self.hasPermission(channel, Permission.MESSAGE_WRITE)) {
            return channel;
        }

        for (TextChannel textChannel : guild.getTextChannels()) {
            if (self.hasPermission(textChannel, Permission.MESSAGE_WRITE)) {
                return textChannel;
            }
        }
        return null;
    }
}
