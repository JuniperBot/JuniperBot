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
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.persistence.entity.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

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

    @Column
    private boolean enabled;

    @Transient
    public boolean isValid() {
        return enabled && StringUtils.isNotEmpty(token) && hookId != null;
    }
}
