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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.dv8tion.jda.api.JDA;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class DiscordBotsOrgStats implements Serializable {
    private static final long serialVersionUID = 7184615746356909641L;

    @JsonProperty("server_count")
    private long serverCount;

    @JsonProperty("shard_id")
    private int shardId;

    @JsonProperty("shard_count")
    private int shardTotal;

    public DiscordBotsOrgStats(JDA shard) {
        JDA.ShardInfo info = shard.getShardInfo();
        shardId = info.getShardId();
        shardTotal = info.getShardTotal();
        serverCount = shard.getGuildCache().size();
    }
}
