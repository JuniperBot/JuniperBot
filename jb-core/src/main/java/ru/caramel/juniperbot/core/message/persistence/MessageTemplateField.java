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
package ru.caramel.juniperbot.core.message.persistence;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.core.common.persistence.base.BaseEntity;
import ru.caramel.juniperbot.core.message.service.MessageService;

import javax.persistence.*;

/**
 * Message template field entity
 *
 * @see MessageTemplate
 * @see MessageService
 */
@Getter
@Setter
@Entity
@Table(name = "message_template_field")
public class MessageTemplateField extends BaseEntity {

    private static final long serialVersionUID = 3637206110866462471L;

    @ManyToOne
    @JoinColumn(name = "message_template_id")
    private MessageTemplate template;

    @Column
    private int index;

    @Column(columnDefinition = "text")
    private String name;

    @Column(columnDefinition = "text")
    private String value;

    @Column
    private boolean inline;

}
