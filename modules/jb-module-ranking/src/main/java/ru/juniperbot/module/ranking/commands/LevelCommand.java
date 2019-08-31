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
package ru.juniperbot.module.ranking.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.utils.RankingUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;

@DiscordCommand(key = "discord.command.level.key",
        description = "discord.command.level.desc",
        group = "discord.command.group.ranking",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS},
        priority = 245)
public class LevelCommand extends RankingCommand {

    @Autowired
    private ModerationService moderationService;

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent event, BotContext context, String content) {
        TextChannel channel = event.getChannel();
        Member mentioned = getMentioned(event);
        if (mentioned == null) {
            return showHelp(channel, context);
        }
        String input = removeMention(content);
        if (!StringUtils.isNumeric(input)) {
            return showHelp(channel, context);
        }

        int level;
        try {
            level = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return showHelp(channel, context);
        }
        if (level < 0 || level > RankingUtils.MAX_LEVEL) {
            return showHelp(channel, context);
        }
        RankingUpdateRequest request = new RankingUpdateRequest(mentioned);
        request.setLevel(level);
        contextService.queue(event.getGuild(), channel.sendTyping(), e -> {
            rankingConfigService.update(request);
            rankingService.updateRewards(mentioned);
            messageService.onTempEmbedMessage(channel, 10, "discord.command.level.success",
                    mentioned.getAsMention(), level);
        });
        return true;
    }

    private boolean showHelp(TextChannel channel, BotContext context) {
        String levelCommand = messageService.getMessageByLocale("discord.command.level.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(channel, "discord.command.level.help",
                RankingUtils.MAX_LEVEL, context.getConfig().getPrefix(), levelCommand);
        return false;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return super.isAvailable(user, member, guild)
                && moderationService.isModerator(member);
    }
}
