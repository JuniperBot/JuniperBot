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
package ru.juniperbot.common.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.juniperbot.common.model.VkConnectionStatus;
import ru.juniperbot.common.persistence.entity.base.BaseSubscriptionEntity;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vk_connection")
public class VkConnection extends BaseSubscriptionEntity {
    private static final long serialVersionUID = 2146901518074674594L;

    @Column(name = "group_id")
    private Integer groupId;

    @Column
    private String token;

    @Column(name = "confirm_code")
    private String confirmCode;

    @Column
    @Enumerated(EnumType.STRING)
    private VkConnectionStatus status;

    @Type(type = "jsonb")
    @Column(name = "attachments", columnDefinition = "json")
    private List<String> attachmentTypes;

    @Column(name = "group_only_posts")
    private boolean groupOnlyPosts;

    @Column(name = "show_post_link")
    private boolean showPostLink = true;

    @Column(name = "show_date")
    private boolean showDate = true;

}
