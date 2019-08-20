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
package ru.juniperbot.common.worker.message.model;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import ru.juniperbot.common.persistence.entity.MessageTemplate;
import ru.juniperbot.common.support.MapPlaceholderResolver;

import java.io.Serializable;

@Getter
public abstract class MessageTemplateCompiler {

    private Guild guild;

    private Member member;

    private TextChannel fallbackChannel;

    private String fallbackContent;

    private boolean directAllowed;

    private final MessageTemplate template;

    private final MapPlaceholderResolver variables = new MapPlaceholderResolver();

    protected MessageTemplateCompiler(MessageTemplate template) {
        this.template = template;
    }

    public MessageTemplateCompiler withGuild(Guild guild) {
        this.guild = guild;
        return this;
    }

    public MessageTemplateCompiler withMember(Member member) {
        this.member = member;
        return this;
    }

    public MessageTemplateCompiler withFallbackChannel(TextChannel fallbackChannel) {
        this.fallbackChannel = fallbackChannel;
        return this;
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

    public abstract String processContent(String content, boolean placeholdersOnly);
}
