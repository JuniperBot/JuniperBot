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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.model.ModerationActionType;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.modules.moderation.model.ModerationActionRequest;

@DiscordCommand(key = "discord.command.mod.kick.key",
        description = "discord.command.mod.kick.desc",
        group = "discord.command.group.moderation",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.KICK_MEMBERS},
        priority = 20)
public class KickCommand extends MentionableModeratorCommand {

    public KickCommand() {
        super(false, true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        Member violator = reference.getMember();
        if (violator == null) {
            showHelp(event, context);
            return false;
        }
        if (!checkTarget(reference, event)) {
            return false;
        }

        if (!event.getGuild().getSelfMember().canInteract(violator)) {
            messageService.onError(event.getChannel(), "discord.command.mod.kick.position");
            return false;
        }

        ModerationActionRequest request = ModerationActionRequest.builder()
                .type(ModerationActionType.KICK)
                .moderator(event.getMember())
                .violator(violator)
                .reason(query)
                .build();

        return moderationService.performAction(request) ? ok(event) : fail(event);
    }

    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String kickCommand = messageService.getMessageByLocale("discord.command.mod.kick.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.kick.help",
                context.getConfig().getPrefix(), kickCommand);
    }
}
