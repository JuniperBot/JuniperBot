/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import ru.juniperbot.common.persistence.entity.WebHook;

import java.util.function.Consumer;

public class WebhookUtils {

    public static void sendWebhook(WebHook webHook, WebhookMessage message, Consumer<WebHook> onAbsent) {
        if (message == null) {
            return;
        }
        WebhookClient client = new WebhookClientBuilder(webHook.getHookId(), webHook.getToken()).build();
        client.send(message).whenComplete((v, e) -> {
            if (e != null && e.getMessage().contains("Request returned failure 404")) {
                onAbsent.accept(webHook);
            }
            client.close();
        });
    }
}
