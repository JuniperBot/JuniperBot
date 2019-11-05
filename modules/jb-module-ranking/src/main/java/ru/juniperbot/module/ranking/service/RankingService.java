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
package ru.juniperbot.module.ranking.service;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.module.ranking.utils.VoiceActivityTracker;

public interface RankingService {

    String COOKIE_EMOTE = "\uD83C\uDF6A";

    void onMessage(GuildMessageReceivedEvent event);

    void giveCookie(Member senderMember, Member recipientMember);

    void addVoiceActivity(Member member, VoiceActivityTracker.MemberState state);

    void updateRewards(Member member);

    RankingInfo getRankingInfo(Member member);

    RankingInfo getRankingInfo(LocalMember member);
}
