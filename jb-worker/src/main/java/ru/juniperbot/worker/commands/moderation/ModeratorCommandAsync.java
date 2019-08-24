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
package ru.juniperbot.worker.commands.moderation;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;
import org.ocpsoft.prettytime.units.Second;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.worker.command.model.AbstractCommandAsync;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;

import java.util.Date;

public abstract class ModeratorCommandAsync extends AbstractCommandAsync {

    @Autowired
    protected ModerationService moderationService;

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return member != null && moderationService.isModerator(member);
    }

    protected String getMuteDuration(int duration) {
        Date date = new Date();
        date.setTime(date.getTime() + (long) (60000 * duration));
        PrettyTime formatter = new PrettyTime(contextService.getLocale());
        formatter.removeUnit(JustNow.class);
        formatter.removeUnit(Millisecond.class);
        formatter.removeUnit(Second.class);
        return messageService.getMessage("discord.command.mod.warn.exceeded.message.MUTE.until",
                formatter.format(date));
    }
}
