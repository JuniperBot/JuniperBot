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
package ru.caramel.juniperbot.core.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.caramel.juniperbot.core.model.enums.MessageTemplateType;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Message template entity used for sending event messages like member join/leave, ranking announce, etc.
 *
 * @see MessageTemplateType
 * @see MessageTemplateField
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "message_template")
public class MessageTemplate extends BaseEntity {

    private static final long serialVersionUID = -831681014535402042L;

    private static final int URL_MAX_LENGTH = 2000;

    @Column
    @Enumerated(EnumType.STRING)
    @NotNull
    private MessageTemplateType type = MessageTemplateType.TEXT;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name="index")
    private List<MessageTemplateField> fields;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "channel_id")
    private String channelId;

    @Column
    private boolean tts;

    @Column(length = 7)
    private String color;

    @Column(name = "image_url", length = URL_MAX_LENGTH)
    public String imageUrl;

    @Column(name = "thumbnail_url", length = URL_MAX_LENGTH)
    public String thumbnailUrl;

    @Column(columnDefinition = "text")
    public String author;

    @Column(name = "author_url", length = URL_MAX_LENGTH)
    public String authorUrl;

    @Column(name = "author_icon_url", length = URL_MAX_LENGTH)
    public String authorIconUrl;

    @Column(columnDefinition = "text")
    public String title;

    @Column(name = "title_url", length = URL_MAX_LENGTH)
    public String titleUrl;

    @Column(columnDefinition = "text")
    public String footer;

    @Column(name = "footer_icon_url", length = URL_MAX_LENGTH)
    public String footerIconUrl;

}
