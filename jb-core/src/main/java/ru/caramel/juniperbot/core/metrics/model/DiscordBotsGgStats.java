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
package ru.caramel.juniperbot.core.metrics.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class DiscordBotsGgStats implements Serializable {
    private static final long serialVersionUID = 7184615746356909641L;

    private long guildCount;

    private int shardId;

    private int shardCount;

    public DiscordBotsGgStats(JDA shard) {
        JDA.ShardInfo info = shard.getShardInfo();
        shardId = info.getShardId();
        shardCount = info.getShardTotal();
        guildCount = shard.getGuildCache().size();
    }
}
