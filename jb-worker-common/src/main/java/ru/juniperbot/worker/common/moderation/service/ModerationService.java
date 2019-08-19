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
package ru.juniperbot.worker.common.moderation.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import ru.juniperbot.worker.common.moderation.model.ModerationActionRequest;
import ru.juniperbot.worker.common.moderation.model.WarningResult;
import ru.juniperbot.common.persistence.entity.MemberWarning;

import java.util.List;

public interface ModerationService {

    boolean isModerator(Member member);

    boolean isPublicColor(long guildId);

    boolean setColor(Member member, String color);

    boolean performAction(ModerationActionRequest request);

    WarningResult warn(Member author, Member member, String reason);

    List<MemberWarning> getWarnings(Member member);

    void removeWarn(MemberWarning warning);

    Member getLastActionModerator(Member violator);

    Member getLastActionModerator(Guild guild, User violator);
}
