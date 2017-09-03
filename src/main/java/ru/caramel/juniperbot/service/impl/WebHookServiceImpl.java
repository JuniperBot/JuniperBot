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
package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.integration.discord.DiscordClient;
import ru.caramel.juniperbot.model.WebHookDto;
import ru.caramel.juniperbot.persistence.entity.WebHook;
import ru.caramel.juniperbot.service.MapperService;
import ru.caramel.juniperbot.service.PermissionsService;
import ru.caramel.juniperbot.service.WebHookService;

import java.util.List;

@Service
public class WebHookServiceImpl implements WebHookService {

    @Autowired
    private MapperService mapper;

    @Autowired
    private DiscordClient discordClient;

    @Autowired
    private PermissionsService permissionsService;

    @Override
    public WebHookDto getDtoForView(long guildId, WebHook webHook) {
        WebHookDto hookDto = mapper.getWebHookDto(webHook);
        if (discordClient.isConnected()) {
            JDA jda = discordClient.getJda();
            Guild guild = jda.getGuildById(guildId);
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
        return hookDto;
    }

    public void updateWebHook(long guildId, Long channelId, WebHook webHook, String name) {
        if (discordClient.isConnected()) {
            JDA jda = discordClient.getJda();
            Guild guild = jda.getGuildById(guildId);
            if (guild != null && channelId != null && permissionsService.hasWebHooksAccess(guild)) {
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook == null) {
                    TextChannel channel = guild.getTextChannelById(channelId);
                    if (permissionsService.hasWebHooksAccess(channel)) {
                        webhook = guild.getController().createWebhook(channel, name).complete();
                    }
                }
                if (webhook != null) {
                    if (!channelId.equals(webhook.getChannel().getIdLong())) {
                        TextChannel channel = guild.getTextChannelById(channelId);
                        if (channel == null) {
                            throw new IllegalStateException("Tried to set non-existent channel");
                        }
                        webhook.getManager().setChannel(channel).complete();
                    }
                    webHook.setHookId(webhook.getIdLong());
                    webHook.setToken(webhook.getToken());
                }

            }
        }
    }

    public boolean delete(long guildId, WebHook webHook) {
        if (discordClient.isConnected()) {
            JDA jda = discordClient.getJda();
            Guild guild = jda.getGuildById(guildId);
            if (guild != null && permissionsService.hasWebHooksAccess(guild)) {
                Webhook webhook = getWebHook(guild, webHook);
                if (webhook != null) {
                    webhook.delete().queue();
                    return true;
                }
            }
        }
        return false;
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
