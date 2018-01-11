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
package ru.caramel.juniperbot.modules.customcommand.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import ru.caramel.juniperbot.modules.customcommand.model.CommandType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
public class CustomCommandDto implements Serializable {
    private static final long serialVersionUID = 6050850842749762636L;

    private Long id;

    @NotNull
    private CommandType type;

    @Size(max = 25, message = "{validation.commands.key.Size.message}")
    @NotBlank(message = "{validation.commands.key.NotBlank.message}")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я0-9]*$", message = "{validation.commands.key.pattern.message}")
    private String key;

    @NotBlank(message = "{validation.commands.content.NotBlank.message}")
    private String content;
}
