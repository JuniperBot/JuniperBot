package ru.caramel.juniperbot.service;

import com.vk.api.sdk.callback.objects.messages.CallbackMessage;
import com.vk.api.sdk.callback.objects.wall.CallbackWallPost;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;

public interface VkService {

    VkConnection create(GuildConfig config, String name, String code);

    void delete(GuildConfig config, long id);

    VkConnection getForToken(String token);

    String confirm(VkConnection connection, CallbackMessage message);

    void post(VkConnection connection, CallbackMessage<CallbackWallPost> message);
}
