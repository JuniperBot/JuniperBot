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
package ru.caramel.juniperbot.module.moderation.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.mute.key",
        description = "discord.command.mod.mute.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_WRITE, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MUTE_OTHERS},
        priority = 20)
public class MuteCommand extends ModeratorCommandAsync {

    @Override
    protected void doCommandAsync(MessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
            return; // do not allow to mute moderators
        }
        if (mentioned == null) {
            messageService.onError(event.getChannel(), "discord.command.mod.mute.mention");
            return;
        }
        boolean global = StringUtils.containsIgnoreCase(event.getMessage().getContentRaw(),
                messageService.getMessage("discord.command.mod.mute.key.everywhere"));
        boolean muted = moderationService.mute(event.getTextChannel(), mentioned, global);
        messageService.onEmbedMessage(event.getChannel(), muted
                ? "discord.command.mod.mute.done" : "discord.command.mod.mute.already", mentioned.getEffectiveName());
    }
}
