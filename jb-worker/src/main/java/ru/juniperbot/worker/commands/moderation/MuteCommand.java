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
package ru.juniperbot.worker.commands.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.model.ModerationActionType;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.modules.moderation.model.ModerationActionRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.mod.mute.key",
        description = "discord.command.mod.mute.desc",
        group = "discord.command.group.moderation",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MUTE_OTHERS},
        priority = 30)
public class MuteCommand extends MentionableModeratorCommand {

    private static final String COMMAND_PATTERN = "^(\\d+\\s*)?(%s\\s*)?(.*)$";

    protected MuteCommand() {
        super(false, true);
    }

    @Override
    protected boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        Member violator = reference.getMember();
        if (violator == null) {
            return fail(event);
        }
        if (!checkTarget(reference, event)) {
            return false;
        }

        String globalKeyWord = messageService.getMessageByLocale("discord.command.mod.mute.key.everywhere",
                context.getCommandLocale());

        Matcher m = Pattern
                .compile(String.format(COMMAND_PATTERN, Pattern.quote(globalKeyWord)))
                .matcher(query);
        if (!m.find()) {
            showHelp(event, context);
            return false;
        }

        Integer duration = null;
        if (StringUtils.isNotBlank(m.group(1))) {
            try {
                duration = Integer.parseInt(m.group(1).trim());
            } catch (NumberFormatException e) {
                showHelp(event, context);
                return false;
            }
        }
        boolean global = m.group(2) != null && globalKeyWord.equals(m.group(2).trim());
        String reason = m.group(3);

        ModerationActionRequest request = ModerationActionRequest.builder()
                .type(ModerationActionType.MUTE)
                .moderator(event.getMember())
                .channel(event.getChannel())
                .violator(violator)
                .global(global)
                .duration(duration)
                .reason(reason)
                .build();

        boolean muted = moderationService.performAction(request);

        EmbedBuilder builder = messageService.getBaseEmbed();
        StringBuilder result = new StringBuilder();
        if (muted) {
            result.append(messageService.getMessage("discord.command.mod.mute.done",
                    violator.getEffectiveName()));
            if (duration != null) {
                result.append("\n").append(getMuteDuration(duration));
            }
            if (StringUtils.isNotEmpty(reason)) {
                builder.addField(messageService.getMessage("audit.reason"),
                        CommonUtils.trimTo(reason, MessageEmbed.VALUE_MAX_LENGTH), true);
            }
        } else {
            result.append(messageService.getMessage("discord.command.mod.mute.already",
                    violator.getEffectiveName()));
        }

        builder.setDescription(result.toString());
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
        return true;
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String globalKeyWord = messageService.getMessageByLocale("discord.command.mod.mute.key.everywhere",
                context.getCommandLocale());
        String muteCommand = messageService.getMessageByLocale("discord.command.mod.mute.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(),
                "discord.command.mod.mute.mention",
                context.getConfig().getPrefix(), muteCommand, globalKeyWord);
    }
}
