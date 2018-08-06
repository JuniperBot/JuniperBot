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
package ru.caramel.juniperbot.module.moderation.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import ru.caramel.juniperbot.module.moderation.persistence.entity.MemberWarning;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;

import java.util.List;

public interface ModerationService {

    ModerationConfig getConfig(Guild guild);

    ModerationConfig getConfig(long serverId);

    ModerationConfig save(ModerationConfig config);

    boolean isModerator(Member member);

    boolean isPublicColor(long serverId);

    boolean setColor(Member member, String color);

    Role getMutedRole(Guild guild);

    boolean mute(TextChannel channel, Member member, boolean global, Integer duration, String reason);

    boolean unmute(TextChannel channel, Member member);

    void refreshMute(Member member);

    void slowMode(TextChannel channel, int interval);

    boolean isRestricted(TextChannel channel, Member member);

    boolean slowOff(TextChannel channel);

    void kick(Member author, Member member);

    void kick(Member author, Member member, String reason);

    void ban(Member author, Member member);

    void ban(Member author, Member member, String reason);

    void ban(Member author, Member member, int dayDel, String reason);

    List<MemberWarning> getWarnings(Member member);

    long warnCount(Member member);

    boolean warn(Member author, Member member);

    boolean warn(Member author, Member member, String reason);

    void removeWarn(MemberWarning warning);

    void clearState(long guildId, String userId, String channelId);

}
