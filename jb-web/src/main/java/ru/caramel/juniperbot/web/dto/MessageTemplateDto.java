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
import ru.caramel.juniperbot.core.model.enums.MessageTemplateType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class MessageTemplateDto implements Serializable {

    private static final long serialVersionUID = 929677926296424697L;

    @NotNull
    private MessageTemplateType type = MessageTemplateType.TEXT;

    private List<MessageTemplateFieldDto> fields;

    private String content;

    private String channelId;

    private boolean tts;

    private String color;

    public String imageUrl;

    public String thumbnailUrl;

    public String author;

    public String authorUrl;

    public String authorIconUrl;

    public String title;

    public String titleUrl;

    public String footer;

    public String footerIconUrl;

}
