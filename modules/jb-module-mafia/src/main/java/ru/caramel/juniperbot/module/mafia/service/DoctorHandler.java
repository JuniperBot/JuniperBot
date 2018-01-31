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
package ru.caramel.juniperbot.module.mafia.service;

import net.dv8tion.jda.core.entities.PrivateChannel;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.mafia.model.*;

@Component
public class DoctorHandler extends IndividualHandler<CopHandler> {

    public DoctorHandler() {
        super(MafiaRole.DOCTOR, MafiaState.NIGHT_DOCTOR);
    }

    @Override
    protected void choiceAction(MafiaInstance instance, MafiaPlayer target, PrivateChannel channel) {
        instance.getDailyActions().put(MafiaActionType.DOCTOR_HEAL, target);
        channel.sendMessage(messageService.getMessage("mafia.doctor.choice.selected", target.getName())).complete();
    }

    @Override
    protected Class<CopHandler> getNextType() {
        return CopHandler.class;
    }
}
