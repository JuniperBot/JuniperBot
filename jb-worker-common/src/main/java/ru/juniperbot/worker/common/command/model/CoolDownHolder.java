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
package ru.juniperbot.worker.common.command.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.persistence.entity.CommandConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class CoolDownHolder {

    private final long guildId;

    private final Map<String, Long> accessMap = new ConcurrentHashMap<>();

    public CoolDownHolder(long guildId) {
        this.guildId = guildId;
    }

    public long perform(GuildMessageReceivedEvent event, CommandConfig commandConfig) {
        if (commandConfig.getCoolDown() == 0) {
            return 0;
        }
        String key = getKey(event, commandConfig);
        long currentMillis = System.currentTimeMillis();
        Long lastDuration = accessMap.get(key);
        if (lastDuration == null) {
            accessMap.put(key, currentMillis);
            return 0;
        }
        long estimate = commandConfig.getCoolDown() * 1000 - currentMillis + lastDuration;
        if (estimate <= 0) {
            accessMap.put(key, currentMillis);
        }
        return estimate;
    }

    private String getKey(GuildMessageReceivedEvent event, CommandConfig commandConfig) {
        StringBuilder builder = new StringBuilder(commandConfig.getKey());
        switch (commandConfig.getCoolDownMode()) {
            case CHANNEL:
                builder.append("-c").append(event.getChannel().getId());
                break;
            case USER:
                builder.append("-u").append(event.getAuthor().getId());
                break;
        }
        return builder.toString();
    }
}
