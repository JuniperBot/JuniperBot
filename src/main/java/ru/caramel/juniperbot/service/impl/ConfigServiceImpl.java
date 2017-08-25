package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.ConfigDto;
import ru.caramel.juniperbot.model.WebHookDto;
import ru.caramel.juniperbot.model.WebHookType;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.persistence.repository.GuildConfigRepository;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.PermissionsService;

import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private GuildConfigRepository repository;

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private MapperService mapper;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private PermissionsService permissionsService;

    @Override
    @Transactional
    public ConfigDto getConfig(long serverId) {
        return getConfigDto(getOrCreate(serverId));
    }

    @Override
    @Transactional
    public void saveConfig(ConfigDto dto, long serverId) {
        GuildConfig config = getOrCreate(serverId);
        updateConfig(dto, config);
        repository.save(config);
    }

    @Override
    @Transactional
    public GuildConfig getOrCreate(long serverId) {
        GuildConfig config = repository.findByGuildId(serverId);

        boolean shouldSave = false;
        if (config == null) {
            config = new GuildConfig(serverId);
            config.setPrefix(discordConfig.getPrefix());
            shouldSave = true;
        }

        if (config.getWebHook() == null) {
            WebHook webHook = new WebHook();
            webHook.setType(WebHookType.INSTAGRAM);
            config.setWebHook(webHook);
            shouldSave = true;
        }

        if (discordClient.isConnected() && (config.getMusicChannelId() == null ||
                discordClient.getJda().getVoiceChannelById(config.getMusicChannelId()) == null)) {
            VoiceChannel channel = discordClient.getDefaultMusicChannel(config.getGuildId());
            if (channel != null) {
                config.setMusicChannelId(channel.getIdLong());
                shouldSave = true;
            }
        }
        return shouldSave ? repository.save(config) : config;
    }

    private ConfigDto getConfigDto(GuildConfig config) {
        ConfigDto dto = mapper.getConfigDto(config);
        WebHook webHook = config.getWebHook();
        WebHookDto hookDto = dto.getWebHook();

        if (discordClient.isConnected()) {
            JDA jda = discordClient.getJda();
            Guild guild = jda.getGuildById(config.getGuildId());
            if (guild != null && permissionsService.hasWebHooksAccess(guild)) {
                hookDto.setAvailable(true);
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook != null) {
                    hookDto.setChannelId(webhook.getChannel().getIdLong());
                } else {
                    hookDto.setEnabled(false);
                }
            }
        }
        return dto;
    }

    private void updateConfig(ConfigDto dto, GuildConfig config) {
        mapper.updateConfig(dto, config);
        WebHook webHook = config.getWebHook();
        WebHookDto hookDto = dto.getWebHook();
        if (hookDto == null) {
            return;
        }

        if (discordClient.isConnected()) {
            JDA jda = discordClient.getJda();
            Guild guild = jda.getGuildById(config.getGuildId());
            if (guild != null && hookDto.getChannelId() != null && permissionsService.hasWebHooksAccess(guild)) {
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook == null) {
                    TextChannel channel = guild.getTextChannelById(hookDto.getChannelId());
                    webhook = guild.getController().createWebhook(channel, "JuniperBot").complete();
                }
                if (!hookDto.getChannelId().equals(webhook.getChannel().getIdLong())) {
                    TextChannel channel = guild.getTextChannelById(hookDto.getChannelId());
                    if (channel == null) {
                        throw new IllegalStateException("Tried to set non-existent channel");
                    }
                    // TODO Update to new JDA because of bug https://github.com/DV8FromTheWorld/JDA/pull/438
                    // webhook.getManager().setChannel(channel).queue();
                    webhook.delete().complete();
                    webhook = guild.getController().createWebhook(channel, "JuniperBot").complete();
                }
                webHook.setHookId(webhook.getIdLong());
                webHook.setToken(webhook.getToken());
            }
        }
    }

    private Webhook getWebHook(Guild guild, WebHook webHook) {
        if (webHook.getHookId() != null && webHook.getToken() != null) {
            List<Webhook> webHooks = guild.getWebhooks().complete();
            return webHooks.stream()
                    .filter(e -> webHook.getHookId().equals(e.getIdLong())
                            && webHook.getToken().equals(e.getToken())).findFirst().orElse(null);
        }
        return null;
    }
}
