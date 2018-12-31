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
package ru.caramel.juniperbot.web.dto.config;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.web.dto.MessageTemplateDto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class WelcomeDto implements Serializable {

    private static final long serialVersionUID = -7504705178352675860L;

    private boolean restoreState;

    private Set<String> joinRoles = new HashSet<>();

    private boolean joinEnabled;

    private MessageTemplateDto joinTemplate;

    private boolean joinDmEnabled;

    private MessageTemplateDto joinDmTemplate;

    private boolean leaveEnabled;

    private MessageTemplateDto leaveTemplate;
}
