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
package ru.caramel.juniperbot.core.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import ru.caramel.juniperbot.core.service.DomainService;
import ru.caramel.juniperbot.core.persistence.entity.MemberWarning;
import ru.caramel.juniperbot.core.persistence.entity.ModerationConfig;

import java.util.List;

public interface ModerationService extends DomainService<ModerationConfig> {

    boolean isModerator(Member member);

    boolean isPublicColor(long guildId);

    boolean setColor(Member member, String color);

    Role getMutedRole(Guild guild);

    boolean mute(Member author, TextChannel channel, Member member, boolean global, Integer duration, String reason);

    boolean unmute(Member author, TextChannel channel, Member member);

    void refreshMute(Member member);

    boolean kick(Member author, Member member);

    boolean kick(Member author, Member member, String reason);

    boolean ban(Member author, Member member);

    boolean ban(Member author, Member member, String reason);

    boolean ban(Member author, Member member, int dayDel, String reason);

    List<MemberWarning> getWarnings(Member member);

    long warnCount(Member member);

    boolean warn(Member author, Member member);

    boolean warn(Member author, Member member, String reason);

    void removeWarn(MemberWarning warning);

    void clearState(long guildId, String userId, String channelId);

}
