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
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mod.unmute.key",
        description = "discord.command.mod.unmute.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS},
        priority = 10)
public class UnMuteCommand extends ModeratorCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (mentioned == null) {
            messageService.onError(event.getChannel(), "discord.command.mod.mute.mention");
            return false;
        }
        boolean unmuted = moderationService.unmute(event.getTextChannel(), mentioned);
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setDescription(messageService.getMessage(unmuted
                ? "discord.command.mod.unmute.done" : "discord.command.mod.unmute.already", mentioned.getEffectiveName()));
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
        return true;
    }
}
