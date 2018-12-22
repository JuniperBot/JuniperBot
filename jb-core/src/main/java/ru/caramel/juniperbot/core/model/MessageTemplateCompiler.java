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
package ru.caramel.juniperbot.core.model;

import lombok.Getter;
import net.dv8tion.jda.core.entities.*;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplate;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;

import java.io.Serializable;

@Getter
public abstract class MessageTemplateCompiler {

    private Long guildId;

    private Long memberId;

    private Long fallbackChannelId;

    private String fallbackContent;

    private boolean directAllowed;

    private final MessageTemplate template;

    private final MapPlaceholderResolver variables = new MapPlaceholderResolver();

    protected MessageTemplateCompiler(MessageTemplate template) {
        this.template = template;
    }

    public MessageTemplateCompiler withGuildId(Long guildId) {
        this.guildId = guildId;
        return this;
    }

    public MessageTemplateCompiler withGuild(Guild guild) {
        return withGuildId(guild != null ? guild.getIdLong() : null);
    }

    public MessageTemplateCompiler withMemberId(Long memberId) {
        this.memberId = memberId;
        return this;
    }

    public MessageTemplateCompiler withMember(Member member) {
        return withMemberId(member != null ? member.getUser().getIdLong() : null);
    }

    public MessageTemplateCompiler withFallbackChannelId(Long fallbackChannelId) {
        this.fallbackChannelId = fallbackChannelId;
        return this;
    }

    public MessageTemplateCompiler withFallbackChannel(TextChannel fallbackChannel) {
        return withFallbackChannelId(fallbackChannel != null ? fallbackChannel.getIdLong() : null);
    }

    public MessageTemplateCompiler withFallbackContent(String fallbackContent) {
        this.fallbackContent = fallbackContent;
        return this;
    }

    public MessageTemplateCompiler withDirectAllowed(boolean directAllowed) {
        this.directAllowed = directAllowed;
        return this;
    }

    public MessageTemplateCompiler withVariable(String key, Serializable value) {
        this.variables.put(key, String.valueOf(value));
        return this;
    }

    public abstract Message compile();

    public abstract void compileAndSend();
}
