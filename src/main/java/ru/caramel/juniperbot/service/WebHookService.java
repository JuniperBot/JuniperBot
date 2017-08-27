package ru.caramel.juniperbot.service;

import ru.caramel.juniperbot.model.WebHookDto;
import ru.caramel.juniperbot.persistence.entity.WebHook;

public interface WebHookService {

    WebHookDto getDtoForView(long guildId, WebHook webHook);

    void updateWebHook(long guildId, Long channelId, WebHook webHook, String name);

    boolean delete(long guildId, WebHook webHook);
}
