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
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.utils.RankingUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.command.model.MentionableCommand;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;
import ru.juniperbot.module.ranking.service.RankingService;

@DiscordCommand(key = "discord.command.level.key",
        description = "discord.command.level.desc",
        group = "discord.command.group.ranking",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS},
        priority = 245)
public class LevelCommand extends MentionableCommand {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private RankingConfigService rankingConfigService;

    @Autowired
    private RankingService rankingService;

    protected LevelCommand() {
        super(false, true);
    }

    @Override
    protected boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String content) throws DiscordException {
        TextChannel channel = event.getChannel();
        LocalMember localMember = reference.getLocalMember();
        if (localMember == null) {
            showHelp(event, context);
            return false;
        }
        if (!StringUtils.isNumeric(content)) {
            showHelp(event, context);
            return false;
        }

        int level;
        try {
            level = Integer.parseInt(content);
        } catch (NumberFormatException e) {
            showHelp(event, context);
            return false;
        }
        if (level < 0 || level > RankingUtils.MAX_LEVEL) {
            showHelp(event, context);
            return false;
        }
        RankingUpdateRequest request = new RankingUpdateRequest(localMember);
        request.setLevel(level);
        contextService.queue(event.getGuild(), channel.sendTyping(), e -> {
            rankingConfigService.update(request);
            Member member = reference.getMember();
            if (member != null) {
                rankingService.updateRewards(reference.getMember());
            }
            messageService.onTempEmbedMessage(channel, 10, "discord.command.level.success",
                    String.format("**%s**", member != null ? member.getAsMention() : localMember.getEffectiveName()), level);
        });
        return true;
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String levelCommand = messageService.getMessageByLocale("discord.command.level.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.level.help",
                RankingUtils.MAX_LEVEL, context.getConfig().getPrefix(), levelCommand);
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return guild != null
                && rankingConfigService.isEnabled(guild.getIdLong())
                && moderationService.isModerator(member);
    }
}
