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
package ru.juniperbot.module.ranking.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.module.ranking.service.RankingService;

@DiscordEvent
public class RankingListener extends DiscordEventListener {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankingConfigService rankingConfigService;

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        taskExecutor.execute(() -> {
            RankingConfig config = rankingConfigService.getByGuildId(event.getGuild().getIdLong());
            if (config != null && config.isResetOnLeave()) {
                rankingConfigService.update(new RankingUpdateRequest(event.getGuild().getIdLong(),
                        event.getMember().getUser().getId(),
                        0,
                        true,
                        true));
            }
        });
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        Member sender = event.getMember();
        if (sender == null
                || sender.getUser().isBot()
                || !event.getReactionEmote().getName().equals(RankingService.COOKIE_EMOTE)) {
            return;
        }
        Member self = event.getGuild().getSelfMember();
        TextChannel channel = event.getChannel();
        if (channel == null || !self.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            return;
        }
        channel.retrieveMessageById(event.getMessageId()).queue(m -> {
            User author = m.getAuthor();
            if (author == null || author.isBot() || author.equals(sender.getUser())) {
                return;
            }
            Member recipient = event.getGuild().getMember(author);
            if (recipient != null) {
                rankingService.giveCookie(sender, recipient);
            }
        });
    }
}
