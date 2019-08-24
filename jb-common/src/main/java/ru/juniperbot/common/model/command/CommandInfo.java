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
package ru.juniperbot.common.model.command;

import lombok.*;
import net.dv8tion.jda.api.Permission;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "Builder")
public class CommandInfo implements Serializable {
    private static final long serialVersionUID = 5138866045108308807L;

    private String key;

    private String description;

    private Map<String, String> keyLocalized;

    private Map<String, String> descriptionLocalized;

    private String[] group;

    private Permission[] permissions;

    private int priority;

    private boolean hidden;
}
