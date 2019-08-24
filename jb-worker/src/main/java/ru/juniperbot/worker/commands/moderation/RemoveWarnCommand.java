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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.persistence.entity.MemberWarning;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;

import java.util.List;
import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.removeWarm.key",
        description = "discord.command.mod.removeWarm.desc",
        group = "discord.command.group.moderation",
        priority = 10)
public class RemoveWarnCommand extends ModeratorCommandAsync {

    @Override
    public void doCommandAsync(GuildMessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        query = removeMention(query);
        if (mentioned == null || !StringUtils.isNumeric(query)) {
            String warnsCommand = messageService.getMessageByLocale("discord.command.mod.warns.key",
                    context.getCommandLocale());
            String removeWarmCommand = messageService.getMessageByLocale("discord.command.mod.removeWarm.key",
                    context.getCommandLocale());
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.removeWarm.help",
                    context.getConfig().getPrefix(), warnsCommand, removeWarmCommand);
            return;
        }
        if (Objects.equals(mentioned, event.getMember())) {
            fail(event); // do not allow remove warns from yourself
            return;
        }

        int index;
        try {
            index = Integer.parseInt(query) - 1;
        } catch (NumberFormatException e) {
            fail(event);
            return;
        }

        List<MemberWarning> warningList = moderationService.getWarnings(mentioned);
        if (index < 0 || warningList.size() <= index) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.removeWarm.empty", index + 1);
            return;
        }
        MemberWarning warning = warningList.get(index);
        moderationService.removeWarn(warning);
        ok(event);
    }
}
