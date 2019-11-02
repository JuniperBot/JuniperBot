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

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.MemberWarning;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;

import java.util.List;
import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.removeWarm.key",
        description = "discord.command.mod.removeWarm.desc",
        group = "discord.command.group.moderation",
        priority = 10)
public class RemoveWarnCommand extends MentionableModeratorCommand {

    @Autowired
    private ModerationService moderationService;

    public RemoveWarnCommand() {
        super(false, true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        LocalMember localMember = reference.getLocalMember();
        if (!StringUtils.isNumeric(query)) {
            showHelp(event, context);
            return false;
        }
        if (event.getMember() != null && Objects.equals(localMember.getUser().getUserId(), event.getMember().getUser().getId())) {
            fail(event); // do not allow remove warns from yourself
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(query) - 1;
        } catch (NumberFormatException e) {
            fail(event);
            return false;
        }

        List<MemberWarning> warningList = moderationService.getWarnings(localMember);
        if (index < 0 || warningList.size() <= index) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.removeWarm.empty", index + 1);
            return false;
        }
        MemberWarning warning = warningList.get(index);
        moderationService.removeWarn(warning);
        ok(event);
        return true;
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String warnsCommand = messageService.getMessageByLocale("discord.command.mod.warns.key",
                context.getCommandLocale());
        String removeWarmCommand = messageService.getMessageByLocale("discord.command.mod.removeWarm.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.removeWarm.help",
                context.getConfig().getPrefix(), warnsCommand, removeWarmCommand);
    }
}
