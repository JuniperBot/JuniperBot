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
package ru.caramel.juniperbot.web.dto.request;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class AuditActionRequest implements Serializable {

    private static final long serialVersionUID = 8175025691876905311L;

    private AuditActionType actionType;

    private String userId;

    private String channelId;

    private Date startDate;

    private Date endDate;

    private Date olderThan;
}
