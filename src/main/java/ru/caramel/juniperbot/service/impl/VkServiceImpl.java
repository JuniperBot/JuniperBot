package ru.caramel.juniperbot.service.impl;

import com.vk.api.sdk.callback.objects.messages.CallbackMessage;
import com.vk.api.sdk.callback.objects.wall.CallbackWallPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.model.VkConnectionStatus;
import ru.caramel.juniperbot.model.WebHookType;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.entity.VkConnection;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.persistence.repository.VkConnectionRepository;
import ru.caramel.juniperbot.service.VkService;

import java.util.UUID;

@Service
public class VkServiceImpl implements VkService {

    @Autowired
    private VkConnectionRepository repository;

    @Override
    @Transactional
    public VkConnection create(GuildConfig config, String name, String code) {
        VkConnection connection = new VkConnection();
        connection.setConfig(config);
        connection.setStatus(VkConnectionStatus.CONFIRMATION);
        connection.setToken(UUID.randomUUID().toString());
        connection.setName(name);
        connection.setConfirmCode(code);

        WebHook hook = new WebHook();
        hook.setType(WebHookType.VK);
        hook.setEnabled(true);
        connection.setWebHook(hook);
        return repository.save(connection);
    }

    @Override
    @Transactional
    public void delete(GuildConfig config, long id) {
        VkConnection connection = repository.getOne(id);
        if (!connection.getConfig().equals(config)) {
            throw new IllegalStateException("Trying to delete not own connection!");
        }
        repository.delete(connection);
    }

    @Override
    public VkConnection getForToken(String token) {
        return repository.findByToken(token);
    }

    @Override
    public String confirm(VkConnection connection, CallbackMessage message) {
        connection.setGroupId(message.getGroupId());
        connection.setStatus(VkConnectionStatus.CONNECTED);
        return repository.save(connection).getConfirmCode();
    }

    @Override
    public void post(VkConnection connection, CallbackWallPost post) {

    }
}
