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
package ru.juniperbot.worker.commands.info;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.List;

@DiscordCommand(
        key = "discord.command.server.key",
        description = "discord.command.server.desc",
        group = "discord.command.group.info",
        priority = 2)
public class ServerInfoCommand extends AbstractInfoCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        Guild guild = message.getGuild();

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.server.title", guild.getName()));
        builder.setThumbnail(guild.getIconUrl());
        builder.setFooter(messageService.getMessage("discord.command.info.identifier", guild.getId()), null);

        builder.addField(getMemberListField(guild));
        builder.addField(getChannelListField(guild));
        builder.addField(getShard(guild));
        builder.addField(getVerificationLevel(guild.getVerificationLevel()));
        builder.addField(getRegion(guild));
        builder.addField(getOwner(guild));
        builder.addField(getCreatedAt(guild, context));

        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    protected MessageEmbed.Field getVerificationLevel(Guild.VerificationLevel level) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.verificationLevel"),
                messageService.getEnumTitle(level), true);
    }

    protected MessageEmbed.Field getOwner(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.owner"),
                DiscordUtils.formatUser(guild.getOwner().getUser()), true);
    }

    protected MessageEmbed.Field getRegion(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.region"),
                messageService.getEnumTitle(guild.getRegion()), true);
    }

    protected MessageEmbed.Field getShard(Guild guild) {
        return new MessageEmbed.Field(messageService.getMessage("discord.command.server.shard.title"),
                String.format("#**%s**", guild.getJDA().getShardInfo().getShardId() + 1), true);
    }

    protected MessageEmbed.Field getCreatedAt(Guild guild, BotContext context) {
        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime()
                .withLocale(contextService.getLocale())
                .withZone(context.getTimeZone());
        return getDateField(guild.getTimeCreated().toEpochSecond(), "discord.command.server.createdAt",
                formatter);
    }

    protected MessageEmbed.Field getChannelListField(Guild guild) {
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

    protected MessageEmbed.Field getMemberListField(Guild guild) {
        List<Member> memberList = guild.getMembers();
        class Info {
            private long userCount = 0;
            private long botCount = 0;
            private long online = 0;
            private long offline = 0;
            private long dnd = 0;
            private long idle = 0;
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
