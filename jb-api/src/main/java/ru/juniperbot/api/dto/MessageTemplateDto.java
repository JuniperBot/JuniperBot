/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.dto;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ChannelType;
import org.springframework.format.annotation.DateTimeFormat;
import ru.juniperbot.api.common.validation.DiscordEntity;
import ru.juniperbot.common.model.MessageTemplateType;
import ru.juniperbot.common.persistence.entity.MessageTemplate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class MessageTemplateDto implements Serializable {

    private static final long serialVersionUID = 929677926296424697L;

    @NotNull
    private MessageTemplateType type = MessageTemplateType.TEXT;

    @Size(max = 25)
    private List<MessageTemplateFieldDto> fields;

    @Size(max = 1800)
    private String content;

    @DiscordEntity(value = ChannelType.TEXT, allowDm = true)
    private String channelId;

    private boolean tts;

    @Pattern(regexp = "^(#[A-Fa-f0-9]{6})?$")
    private String color;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String imageUrl;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String thumbnailUrl;

    @Size(max = MessageTemplate.TITLE_MAX_LENGTH)
    public String author;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String authorUrl;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String authorIconUrl;

    @Size(max = MessageTemplate.TITLE_MAX_LENGTH)
    public String title;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String titleUrl;

    @Size(max = 1800)
    public String footer;

    @Size(max = MessageTemplate.URL_MAX_LENGTH)
    public String footerIconUrl;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Date timestamp;

}
