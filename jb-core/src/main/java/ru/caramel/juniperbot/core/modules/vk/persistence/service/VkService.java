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
package ru.caramel.juniperbot.core.modules.vk.persistence.service;

import com.vk.api.sdk.callback.objects.messages.CallbackMessage;
import com.vk.api.sdk.callback.objects.wall.CallbackWallPost;
import ru.caramel.juniperbot.core.modules.vk.persistence.entity.VkConnection;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;

public interface VkService {

    VkConnection create(GuildConfig config, String name, String code);

    void delete(GuildConfig config, long id);

    VkConnection getForToken(String token);

    String confirm(VkConnection connection, CallbackMessage message);

    void post(VkConnection connection, CallbackMessage<CallbackWallPost> message);
}
