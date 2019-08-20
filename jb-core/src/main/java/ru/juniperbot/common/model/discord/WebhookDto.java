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
package ru.juniperbot.common.model.discord;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDto implements Serializable {

    private static final long serialVersionUID = -4395106779698589967L;

    public static final WebhookDto EMPTY = new WebhookDto();

    private Long id;

    private Long guildId;

    private String channelId;

    private Long webhookId;

    private String name;

    private String token;

    private String iconUrl;
}
