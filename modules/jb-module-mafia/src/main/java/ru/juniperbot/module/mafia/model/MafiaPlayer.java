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
package ru.juniperbot.module.mafia.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

@Getter
@Setter
public class MafiaPlayer {

    private static final int MAX_HEALTH = 2;

    private final JDA jda;

    private final long guildId;

    private final long memberId;

    private MafiaRole role;

    private boolean alive = true;

    private int health = MAX_HEALTH;

    public MafiaPlayer(@NonNull Member member) {
        this.jda = member.getJDA();
        this.guildId = member.getGuild().getIdLong();
        this.memberId = member.getUser().getIdLong();
    }

    public Member getMember() {
        Guild guild = jda.getGuildById(guildId);
        return guild != null ? guild.getMemberById(memberId) : null;
    }

    public User getUser() {
        return jda.getUserById(memberId);
    }

    public void out() {
        alive = false;
    }

    public int damage() {
        return MafiaRole.DOCTOR.equals(role) ? health : --health;
    }

    public void heal() {
        health = MAX_HEALTH;
    }

    public boolean isHealthy() {
        return MAX_HEALTH == health;
    }

    public String getName() {
        Member member = getMember();
        if (member != null) {
            return member.getEffectiveName();
        }
        User user = jda.getUserById(memberId);
        return user != null ? user.getName() : String.valueOf(memberId);
    }

    public String getAsMention() {
        Member member = getMember();
        if (member != null) {
            return member.getAsMention();
        }
        return "<@" + memberId + ">";
    }
}
