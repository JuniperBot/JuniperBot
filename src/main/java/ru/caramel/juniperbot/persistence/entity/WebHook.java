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
package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.model.enums.WebHookType;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "web_hook")
@ToString
@Getter
@Setter
public class WebHook extends BaseEntity {

    private static final long serialVersionUID = 5589056134859236418L;

    @Column(name = "hook_id")
    private Long hookId;

    @Column(name = "token")
    private String token;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private WebHookType type;

    @Column
    private boolean enabled;

    @Transient
    public boolean isValid() {
        return enabled && StringUtils.isNotEmpty(token) && hookId != null;
    }
}
