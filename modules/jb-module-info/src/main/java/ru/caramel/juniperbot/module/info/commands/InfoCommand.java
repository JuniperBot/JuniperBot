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
package ru.caramel.juniperbot.module.info.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.service.ConfigService;
import ru.caramel.juniperbot.core.service.MessageService;

@DiscordCommand(key = "discord.command.info.key",
        description = "discord.command.info.desc",
        group = "discord.command.group.info",
        priority = 0)
public class InfoCommand extends AbstractInfoCommand {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConfigService configService;

    @Value("${jda.version}")
    private String jdaVersion;

    @Value("${spring.version}")
    private String springVersion;

    @Value("${app.version}")
    private String appVersion;

    private DateTime buildTimestamp;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        String prefix = context.getConfig() != null ? context.getConfig().getPrefix() : configService.getDefaultPrefix();

        EmbedBuilder builder = messageService.getBaseEmbed(true);
        builder.setAuthor(message.getJDA().getSelfUser().getName(), messageService.getMessage("about.support.page"));
        builder.setThumbnail(message.getJDA().getSelfUser().getAvatarUrl());
        builder.setDescription(messageService.getMessage("discord.command.info.description", prefix));

        builder.addField(
                messageService.getMessage("discord.command.info.author.title"),
                messageService.getMessage("about.support.owner"), true);

        DateTimeFormatter formatter = DateTimeFormat.shortDateTime().withLocale(contextService.getLocale());
        builder.addField(
                messageService.getMessage("discord.command.info.version.title"),
                String.format("%s (%s)", appVersion, formatter.print(buildTimestamp)), true);

        builder.addField(
                messageService.getMessage("discord.command.info.links.title"),
                messageService.getMessage("discord.command.info.links.content"), true);

        builder.addField(
                messageService.getMessage("discord.command.info.framework.title"),
                messageService.getMessage("discord.command.info.framework.content", jdaVersion, springVersion),
                true);

        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    @Value("${build.timestamp}")
    public void setBuildTimestamp(Long value) {
        buildTimestamp = new DateTime(value).withZone(DateTimeZone.UTC);
    }
}
