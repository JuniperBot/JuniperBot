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
package ru.caramel.juniperbot.core.audit.model;

import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import ru.caramel.juniperbot.core.audit.persistence.AuditAction;
import ru.caramel.juniperbot.core.common.persistence.LocalMember;
import ru.caramel.juniperbot.core.common.persistence.LocalUser;
import ru.caramel.juniperbot.core.common.persistence.base.NamedReference;

import java.util.Date;
import java.util.HashMap;

public abstract class AuditActionBuilder {

    @Getter(AccessLevel.NONE)
    protected final AuditAction action;

    protected AuditActionBuilder(long guildId, AuditActionType actionType) {
        this.action = new AuditAction(guildId);
        this.action.setActionDate(new Date());
        this.action.setActionType(actionType);
        this.action.setAttributes(new HashMap<>());
    }

    public AuditActionBuilder withUser(User user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(Member user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(LocalUser user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(LocalMember user) {
        this.action.setUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(User user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(Member user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(LocalUser user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(LocalMember user) {
        this.action.setTargetUser(getReference(user));
        return this;
    }

    public AuditActionBuilder withChannel(GuildChannel channel) {
        this.action.setChannel(getReference(channel));
        return this;
    }

    public AuditActionBuilder withAttribute(String key, Object value) {
        this.action.getAttributes().put(key, value);
        return this;
    }

    private NamedReference getReference(User user) {
        return user != null ? new NamedReference(user.getId(), user.getName()) : null;
    }

    private NamedReference getReference(LocalUser user) {
        return user != null ? new NamedReference(user.getUserId(), user.getName()) : null;
    }

    private NamedReference getReference(Member member) {
        return member != null ? new NamedReference(member.getUser().getId(), member.getEffectiveName()) : null;
    }

    private NamedReference getReference(LocalMember member) {
        return member != null ? new NamedReference(member.getUser().getUserId(), member.getEffectiveName()) : null;
    }

    private NamedReference getReference(GuildChannel channel) {
        return channel != null ? new NamedReference(channel.getId(), channel.getName()) : null;
    }

    public abstract AuditAction save();
}
