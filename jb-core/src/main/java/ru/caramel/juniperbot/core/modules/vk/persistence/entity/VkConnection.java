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
package ru.caramel.juniperbot.core.modules.vk.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.core.modules.vk.model.VkConnectionStatus;
import ru.caramel.juniperbot.core.modules.webhook.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "vk_connection")
public class VkConnection extends BaseEntity {
    private static final long serialVersionUID = 2146901518074674594L;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "web_hook_id")
    private WebHook webHook;

    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_config_id")
    private GuildConfig config;

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

}
