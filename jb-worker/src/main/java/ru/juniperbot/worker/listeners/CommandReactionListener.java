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
package ru.juniperbot.worker.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.EmojiRole;
import ru.juniperbot.common.persistence.entity.CommandReaction;
import ru.juniperbot.common.persistence.entity.CustomCommand;
import ru.juniperbot.common.persistence.repository.CommandReactionRepository;
import ru.juniperbot.common.service.TransactionHandler;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.common.worker.feature.service.FeatureSetService;

import javax.annotation.Nonnull;

@DiscordEvent(priority = 0)
public class CommandReactionListener extends DiscordEventListener {

    @Autowired
    private CommandReactionRepository reactionRepository;

    @Autowired
    private FeatureSetService featureSetService;

    @Autowired
    private TransactionHandler transactionHandler;

    @Override
    public void onGenericGuildMessageReaction(@Nonnull GenericGuildMessageReactionEvent event) {
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();

        if (event.getMember().getUser().isBot()
                || !self.hasPermission(Permission.MANAGE_ROLES)
                || !featureSetService.isAvailable(guild)) {
            return;
        }

        CommandReaction reaction = reactionRepository.findByMessageId(event.getMessageIdLong());
        if (reaction == null) {
            return;
        }
        CustomCommand customCommand = reaction.getCommand();
        if (CollectionUtils.isEmpty(customCommand.getEmojiRoles())) {
            return;
        }

        EmojiRole emojiRole = customCommand.getEmojiRoles().stream()
                .filter(e -> {
                    if (StringUtils.isEmpty(e.getEmoji()) || StringUtils.isEmpty(e.getRoleId())) {
                        return false;
                    }
                    return e.getEmoji().equals(event.getReactionEmote().isEmoji()
                            ? event.getReactionEmote().getName()
                            : event.getReactionEmote().getId());
                })
                .findFirst()
                .orElse(null);

        if (emojiRole == null) {
            return;
        }

        Role targetRole = guild.getRoleById(emojiRole.getRoleId());
        if (targetRole == null || !self.canInteract(targetRole)) {
            return;
        }
        if (event instanceof GuildMessageReactionAddEvent) {
            guild.addRoleToMember(event.getMember(), targetRole).queue();
        } else {
            guild.removeRoleFromMember(event.getMember(), targetRole).queue();
        }
    }

    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        transactionHandler.runInTransaction(() -> reactionRepository.deleteByMessageId(event.getMessageIdLong()));
    }
}