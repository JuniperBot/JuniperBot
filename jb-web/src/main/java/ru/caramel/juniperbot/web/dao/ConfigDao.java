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
package ru.caramel.juniperbot.web.dao;

import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.persistence.entity.WebHook;
import ru.caramel.juniperbot.core.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.WebHookService;
import ru.caramel.juniperbot.module.audio.persistence.entity.MusicConfig;
import ru.caramel.juniperbot.module.audio.persistence.repository.MusicConfigRepository;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.module.junipost.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.junipost.persistence.repository.JuniPostRepository;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;
import ru.caramel.juniperbot.module.moderation.service.ModerationService;
import ru.caramel.juniperbot.module.vk.model.VkConnectionStatus;
import ru.caramel.juniperbot.module.vk.persistence.entity.VkConnection;
import ru.caramel.juniperbot.module.vk.persistence.repository.VkConnectionRepository;
import ru.caramel.juniperbot.web.dto.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigDao extends AbstractDao {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private MusicConfigRepository musicConfigRepository;

    @Autowired
    private VkConnectionRepository vkConnectionRepository;

    @Autowired
    private JuniPostRepository juniPostRepository;

    @Autowired
    private WebHookService webHookService;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private WebHookDao webHookDao;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ModerationService moderationService;

    @Transactional
    public ConfigDto getConfig(long serverId) {
        GuildConfig config = configService.getOrCreate(serverId);
        ConfigDto dto = mapper.getConfigDto(config);

        JuniPost juniPost = getJuniPost(config);
        WebHookDto hookDto = webHookDao.getDtoForView(config.getGuildId(), juniPost.getWebHook());
        dto.setWebHook(hookDto);

        List<VkConnection> vkConnections = vkConnectionRepository.findAllByGuildId(serverId);
        List<VkConnectionDto> vkConnectionDtos = mapper.getVkConnectionDtos(vkConnections);
        if (CollectionUtils.isNotEmpty(vkConnections)) {
            for (VkConnection connection : vkConnections) {
                WebHookDto vkHookDto = webHookDao.getDtoForView(config.getGuildId(), connection.getWebHook());
                vkConnectionDtos.stream()
                        .filter(e -> e.getId().equals(connection.getId()))
                        .findFirst()
                        .ifPresent(e -> e.setWebHook(vkHookDto));
            }
        }
        dto.setVkConnections(vkConnectionDtos);

        MusicConfig musicConfig = playerService.getConfig(serverId);
        MusicConfigDto musicConfigDto = mapper.getMusicDto(musicConfig);
        if (discordService.isConnected(serverId) && (musicConfigDto.getChannelId() == null ||
                discordService.getShardManager().getVoiceChannelById(musicConfigDto.getChannelId()) == null)) {
            VoiceChannel channel = discordService.getDefaultMusicChannel(config.getGuildId());
            if (channel != null) {
                musicConfigDto.setChannelId(channel.getIdLong());
            }
        }
        dto.setMusicConfig(musicConfigDto);

        ModerationConfig modConfig = moderationService.getConfig(serverId);
        ModerationConfigDto modConfigDto = mapper.getModerationDto(modConfig);
        dto.setModConfig(modConfigDto);
        return dto;
    }

    private JuniPost getJuniPost(GuildConfig config) {
        JuniPost juniPost = juniPostRepository.findByGuildConfig(config);
        if (juniPost == null) {
            juniPost = new JuniPost();
            juniPost.setGuildConfig(config);
            juniPost.setWebHook(new WebHook());
            juniPostRepository.save(juniPost);
        }
        return juniPost;
    }

    @Transactional
    public void saveConfig(ConfigDto dto, long serverId) {
        GuildConfig config = configService.getOrCreate(serverId);
        mapper.updateConfig(dto, config);

        // update music config
        MusicConfigDto musicConfigDto = dto.getMusicConfig();
        MusicConfig musicConfig = playerService.getConfig(serverId);
        mapper.updateMusicConfig(musicConfigDto, musicConfig);
        musicConfigRepository.save(musicConfig);

        // update mod config
        ModerationConfigDto modConfigDto = dto.getModConfig();
        ModerationConfig modConfig = moderationService.getConfig(serverId);
        mapper.updateModerationConfig(modConfigDto, modConfig);
        moderationService.save(modConfig);

        // update webhook config
        JuniPost juniPost = getJuniPost(config);
        WebHook webHook = juniPost.getWebHook();
        WebHookDto hookDto = dto.getWebHook();
        if (hookDto != null) {
            mapper.updateWebHook(hookDto, webHook);
            webHookService.updateWebHook(config.getGuildId(), hookDto.getChannelId(), webHook, "JuniperBot");
        }

        List<VkConnection> vkConnections = vkConnectionRepository.findAllByGuildId(serverId);
        if (dto.getVkConnections() != null) {
            Map<VkConnection, Long> updateMap = new HashMap<>();
            dto.getVkConnections().forEach(e -> {
                VkConnection connection = vkConnections.stream()
                        .filter(e1 -> e.getId().equals(e1.getId()))
                        .findFirst().orElse(null);
                if (connection != null
                        && e.getWebHook() != null
                        && e.getWebHook().getChannelId() != null
                        && VkConnectionStatus.CONNECTED.equals(connection.getStatus())) {
                    mapper.updateWebHook(e.getWebHook(), connection.getWebHook());
                    updateMap.put(connection, e.getWebHook().getChannelId());
                }
            });
            updateMap.forEach((k, v) -> webHookService.updateWebHook(config.getGuildId(), v, k.getWebHook(), k.getName()));
        }
        webHookService.invalidateCache(config.getGuildId());
        repository.save(config);
    }
}
