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
package ru.juniperbot.worker.common.command.service;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.stereotype.Service;
import ru.juniperbot.worker.common.command.model.CoolDownHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoolDownServiceImpl implements CoolDownService {

    @Getter
    private Map<Long, CoolDownHolder> coolDownHolderMap = new ConcurrentHashMap<>();

    @Override
    public void clear(Guild guild) {
        coolDownHolderMap.remove(guild.getIdLong());
    }
}
