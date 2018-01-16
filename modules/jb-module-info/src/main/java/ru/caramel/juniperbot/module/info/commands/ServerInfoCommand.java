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

import java.util.List;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.enums.CommandSource;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.utils.CommonUtils;

@DiscordCommand(
        key = "discord.command.server.key",
        description = "discord.command.server.desc",
        group = "discord.command.group.info",
        source = CommandSource.GUILD,
        priority = 1)
public class ServerInfoCommand extends InfoCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        Guild guild = message.getGuild();

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.server.title", guild.getName()));
        builder.setThumbnail(guild.getIconUrl());
        builder.setFooter(messageService.getMessage("discord.command.info.identifier", guild.getId()), null);

        builder.addField(getMemberListField(guild));
        builder.addField(getChannelListField(guild));
        builder.addField(getVerificationLevel(guild));
        builder.addField(getRegion(guild));
        builder.addField(getOwner(guild));
        builder.addField(getCreatedAt(guild));

        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    private MessageEmbed.Field getVerificationLevel(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.verificationLevel"),
                messageService.getEnumTitle(guild.getVerificationLevel()), true);
    }

    private MessageEmbed.Field getOwner(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.owner"),
                CommonUtils.formatUser(guild.getOwner().getUser()), true);
    }

    private MessageEmbed.Field getRegion(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.region"),
                messageService.getEnumTitle(guild.getRegion()), true);
    }

    private MessageEmbed.Field getCreatedAt(Guild guild) {
        DateTimeFormatter formatter = DateTimeFormat.fullDateTime().withLocale(contextService.getLocale());
        return getDateField(guild.getCreationTime().toEpochSecond(), "discord.command.server.createdAt",
                formatter);
    }

    private MessageEmbed.Field getChannelListField(Guild guild) {
        long total = guild.getTextChannels().size() + guild.getVoiceChannels().size();
        StringBuilder memberBuilder = new StringBuilder();
        if (!guild.getTextChannels().isEmpty()) {
            memberBuilder.append(messageService.getMessage("discord.command.server.channels.text",
                    guild.getTextChannels().size())).append("\n");
        }
        if (!guild.getVoiceChannels().isEmpty()) {
            memberBuilder.append(messageService.getMessage("discord.command.server.channels.voice",
                    guild.getVoiceChannels().size())).append("\n");
        }
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.channels", total),
                memberBuilder.toString(), true);
    }

    private MessageEmbed.Field getMemberListField(Guild guild) {
        List<Member> memberList = guild.getMembers();
        class Info {
            long userCount = 0;
            long botCount = 0;
            long online = 0;
            long offline = 0;
            long dnd = 0;
            long idle = 0;
        }
        Info memberInfo = new Info();
        guild.getMembers().forEach(member -> {
            User user = member.getUser();
            if (user.isBot()) {
                memberInfo.botCount++;
            } else {
                memberInfo.userCount++;
            }
            switch (member.getOnlineStatus()) {
                case ONLINE:
                    memberInfo.online++;
                    break;
                case OFFLINE:
                case INVISIBLE:
                    memberInfo.offline++;
                    break;
                case IDLE:
                    memberInfo.idle++;
                    break;
                case DO_NOT_DISTURB:
                    memberInfo.dnd++;
                    break;
            }
        });
        StringBuilder memberBuilder = new StringBuilder();
        if (memberInfo.userCount > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.users", memberInfo.userCount))
                    .append("\n");
        }
        if (memberInfo.botCount > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.bots", memberInfo.botCount))
                    .append("\n");
        }
        if (memberInfo.online > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.status.online", memberInfo.online))
                    .append("\n");
        }
        if (memberInfo.idle > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.status.idle", memberInfo.idle))
                    .append("\n");
        }
        if (memberInfo.dnd > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.status.dnd", memberInfo.dnd))
                    .append("\n");
        }
        if (memberInfo.offline > 0) {
            memberBuilder.append(messageService.getMessage("discord.command.server.members.status.offline", memberInfo.offline))
                    .append("\n");
        }
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.members", memberList.size()),
                memberBuilder.toString(), true);
    }
}
