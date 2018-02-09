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

import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.module.vk.model.VkConnectionStatus;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VkConnectionDto implements Serializable {

    private static final long serialVersionUID = -3440888869725084354L;

    @NotNull
    private Long id;

    private String token;

    private String name;

    private VkConnectionStatus status;

    private List<WallpostAttachmentType> attachments;

    @Valid
    private WebHookDto webHook;

}
