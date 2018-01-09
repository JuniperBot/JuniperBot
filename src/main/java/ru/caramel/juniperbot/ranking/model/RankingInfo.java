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
package ru.caramel.juniperbot.ranking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.persistence.entity.LocalMember;

@Getter
@Setter
public class RankingInfo {

    private String id;

    private String name;

    private String discriminator;

    private String nick;

    private String avatarUrl;

    private int level;

    private long remainingExp;

    private long levelExp;

    private long totalExp;

    private int rank;

    @JsonIgnore
    private int totalMembers;

    public RankingInfo(LocalMember member) {
        this.id = member.getUserId();
        this.name = member.getName();
        this.discriminator = member.getDiscriminator();
        this.nick = member.getEffectiveName();
        this.avatarUrl = member.getAvatarUrl();
    }

    public int getPct() {
        return (int) (((double) remainingExp / (double) levelExp) * 100);
    }
}
