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
package ru.caramel.juniperbot.module.vk.persistence.entity;

import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.persistence.entity.WebHookOwnedEntity;
import ru.caramel.juniperbot.module.vk.model.VkConnectionStatus;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vk_connection")
public class VkConnection extends WebHookOwnedEntity {
    private static final long serialVersionUID = 2146901518074674594L;

    @Column(name = "group_id")
    private Integer groupId;

    @Column
    private String token;

    @Column
    private String name;

    @Column(name = "confirm_code")
    private String confirmCode;

    @Column
    @Enumerated(EnumType.STRING)
    private VkConnectionStatus status;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<WallpostAttachmentType> attachments;

}
