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
package ru.caramel.juniperbot.module.mafia.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.Objects;

@Getter
@Setter
public class MafiaPlayer {

    private static final int MAX_HEALTH = 2;

    private final Member member;

    private MafiaRole role;

    private boolean alive = true;

    private int health = MAX_HEALTH;

    public MafiaPlayer(Member member) {
        Objects.requireNonNull(member);
        this.member = member;
    }

    public User getUser() {
        return member.getUser();
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
        return member.getEffectiveName();
    }

    public String getAsMention() {
        return member.getAsMention();
    }
}
