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
import org.slf4j.MDC;

import java.io.Serializable;

@Getter
public class ErrorDetailsDto implements Serializable {

    private static final long serialVersionUID = -5105826145748518821L;

    private final String error;

    private final String description;

    private final String requestId;

    public ErrorDetailsDto(Exception e) {
        this.error = e.getClass().getName();
        this.description = e.getMessage();
        this.requestId = MDC.get("requestId");
    }
}
