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
package ru.caramel.juniperbot.web.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vk.api.sdk.callback.objects.messages.CallbackMessage;
import com.vk.api.sdk.callback.objects.messages.CallbackMessageType;
import com.vk.api.sdk.callback.objects.wall.CallbackWallPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.utils.GsonUtils;
import ru.caramel.juniperbot.module.vk.persistence.entity.VkConnection;
import ru.caramel.juniperbot.module.vk.service.VkService;
import ru.caramel.juniperbot.web.controller.front.AbstractController;
import ru.caramel.juniperbot.web.dto.VkConnectionDto;
import ru.caramel.juniperbot.web.service.MapperService;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class VkCallbackController extends AbstractController {

    private static final Logger LOG = LoggerFactory.getLogger(VkCallbackController.class);

    private final Gson gson = GsonUtils.create();

    private final static Map<String, Type> CALLBACK_TYPES;

    static {
        Map<String, Type> types = new HashMap<>();

        types.put(CallbackMessageType.WALL_POST_NEW.getValue(), new TypeToken<CallbackMessage<CallbackWallPost>>() {
        }.getType());

        types.put(CallbackMessageType.CONFIRMATION.getValue(), new TypeToken<CallbackMessage>() {
        }.getType());

        CALLBACK_TYPES = Collections.unmodifiableMap(types);
    }

    @Autowired
    private VkService vkService;

    @Autowired
    private MapperService mapperService;

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/vk/callback/{token}", method = RequestMethod.POST)
    public String callback(@RequestBody String content, @PathVariable("token") String token, HttpServletResponse response) {
        JsonObject json = gson.fromJson(content, JsonObject.class);
        String type = json.get("type").getAsString();
        Type typeOfClass = CALLBACK_TYPES.get(type);

        VkConnection connection = vkService.getForToken(token);
        if (connection == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        if (typeOfClass != null) {
            CallbackMessage message = gson.fromJson(json, typeOfClass);

            if (!CallbackMessageType.CONFIRMATION.equals(message.getType()) &&
                    !connection.getGroupId().equals(message.getGroupId())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }

            switch (message.getType()) {
                case CONFIRMATION:
                    return vkService.confirm(connection, message);
                case WALL_POST_NEW:
                    vkService.post(connection, message);
                    break;
            }
        }
        return "ok";
    }

    @RequestMapping(value = "/vk/create/{serverId}", method = RequestMethod.POST)
    @ResponseBody
    public VkConnectionDto create(
            @PathVariable("serverId") long serverId,
            @RequestParam("name") String name,
            @RequestParam("code") String code) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        return mapperService.getVkConnectionDto(vkService.create(config, name, code));
    }

    @RequestMapping(value = "/vk/delete/{serverId}", method = RequestMethod.POST)
    public void delete(
            @PathVariable("serverId") long serverId,
            @RequestParam("id") long id) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        vkService.delete(config, id);
    }
}
