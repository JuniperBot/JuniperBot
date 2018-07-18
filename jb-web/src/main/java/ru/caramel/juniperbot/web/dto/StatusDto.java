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
package ru.caramel.juniperbot.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class StatusDto implements Serializable {

    private static final long serialVersionUID = 1569031085142209018L;

    private long guildCount;

    private long userCount;

    private long textChannelCount;

    private long voiceChannelCount;

    private long activeConnections;

    private long uptimeDuration;

    private long executedCommands;

    private double commandsRateMean;

    private double commandsRate1m;

    private double commandsRate5m;

    private double commandsRate15m;
}
