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
package ru.juniperbot.common.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.persistence.entity.base.MemberMessageEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "message_history")
public class MessageHistory extends MemberMessageEntity {
    private static final long serialVersionUID = -1459894653981742651L;

    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date createDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date updateDate;

    @Column(name = "message_id")
    @NotNull
    private String messageId;

}
