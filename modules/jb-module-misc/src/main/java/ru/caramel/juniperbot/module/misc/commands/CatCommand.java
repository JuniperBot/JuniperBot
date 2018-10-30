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
package ru.caramel.juniperbot.module.misc.commands;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.model.AbstractCommandAsync;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.module.misc.model.CatResponse;
import ru.caramel.juniperbot.module.misc.model.DogBreedsResponse;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@DiscordCommand(key = "discord.command.cat.key",
        description = "discord.command.cat.desc",
        group = "discord.command.group.fun",
        priority = 17)
public class CatCommand extends AbstractCommandAsync {

    private final static String ENDPOINT = "http://aws.random.cat/meow";

    private RestTemplate restTemplate;

    private final Supplier<CatResponse> catResult =
            Suppliers.memoizeWithExpiration(this::getCat, 1, TimeUnit.SECONDS);

    @PostConstruct
    public void init() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(3000);
        this.restTemplate = new RestTemplate(httpRequestFactory);
    }

    private CatResponse getCat() {
        ResponseEntity<CatResponse> response = restTemplate.getForEntity(ENDPOINT, CatResponse.class);
        if (response.getStatusCode() != HttpStatus.OK
                || response.getBody() == null
                || StringUtils.isEmpty(response.getBody().getFile())) {
            return null;
        }
        return response.getBody();
    }

    @Override
    public void doCommandAsync(MessageReceivedEvent message, BotContext context, String query) {
        try {
            message.getChannel().sendTyping().queue();
            CatResponse response = catResult.get();
            if (response == null) {
                messageService.onEmbedMessage(message.getChannel(), "discord.command.cat.error");
                return;
            }
            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.setImage(response.getFile());
            builder.setColor(null);
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        } catch (HttpClientErrorException e) {
            messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.error");
        }
    }
}
