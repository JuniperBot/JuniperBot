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
import ru.juniperbot.common.model.AuditActionType;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class AuditActionDto implements Serializable {
    private static final long serialVersionUID = -6243311763429092143L;

    private long id;

    private Date actionDate;

    private AuditActionType actionType;

    private NamedReferenceDto user;

    private NamedReferenceDto targetUser;

    private NamedReferenceDto channel;

    private Map<String, Object> attributes;
}
