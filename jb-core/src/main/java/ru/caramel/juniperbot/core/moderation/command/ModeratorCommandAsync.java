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
package ru.caramel.juniperbot.core.moderation.command;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.command.model.AbstractCommandAsync;
import ru.caramel.juniperbot.core.moderation.service.ModerationService;

public abstract class ModeratorCommandAsync extends AbstractCommandAsync {

    @Autowired
    protected ModerationService moderationService;

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return member != null && moderationService.isModerator(member);
    }
}
