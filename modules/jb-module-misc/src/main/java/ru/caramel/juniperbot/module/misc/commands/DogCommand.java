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
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.misc.model.DogBreedsResponse;
import ru.caramel.juniperbot.module.misc.model.DogResponse;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DiscordCommand(key = "discord.command.dog.key",
        description = "discord.command.dog.desc",
        group = "discord.command.group.fun",
        priority = 16)
public class DogCommand extends AbstractCommand {

    private final static String ENDPOINT_ALL = "https://dog.ceo/api/breeds/image/random";

    private final static String ENDPOINT_BREEDS = "https://dog.ceo/api/breeds/list/all";

    private final static String ENDPOINT_BREED = "https://dog.ceo/api/breed/{breed}/images/random";

    private final static String ENDPOINT_SUB_BREED = "https://dog.ceo/api/breed/{breed}/{sub}/images/random";

    private RestTemplate restTemplate;

    private final Supplier<DogBreedsResponse> breedsResult =
            Suppliers.memoizeWithExpiration(this::getBreeds, 5, TimeUnit.HOURS);

    @PostConstruct
    public void init() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(3000);
        this.restTemplate = new RestTemplate(httpRequestFactory);
    }

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        String keyWord = messageService.getMessageByLocale("discord.command.dog.breeds", context.getCommandLocale());
        if (keyWord.equalsIgnoreCase(query)) {
            return showBreeds(message);
        }


        if (StringUtils.isBlank(query)) {
            return execute(message, ENDPOINT_ALL, Collections.emptyMap());
        }
        if (StringUtils.isAlpha(query)) {
            return execute(message, ENDPOINT_BREED, Map.of("breed", query));
        }
        String[] parts = query.split(" ");
        if (parts.length == 2 && StringUtils.isAlpha(parts[0]) && StringUtils.isAlpha(parts[1])) {
            return execute(message, ENDPOINT_SUB_BREED, Map.of("breed", parts[1], "sub", parts[0]));
        }

        String commandKey = messageService.getMessageByLocale("discord.command.dog.key", context.getCommandLocale());
        String prefix = context.getConfig() != null ? context.getConfig().getPrefix() : configService.getDefaultPrefix();

        messageService.onMessage(message.getChannel(), "discord.command.dog.help", prefix, commandKey, keyWord);
        return false;
    }

    private boolean showBreeds(MessageReceivedEvent message) {
        try {
            DogBreedsResponse breeds = breedsResult.get();
            StringBuilder builder = new StringBuilder("markdown\n");
            breeds.getMessage().forEach((breed, subBreeds) -> {
                builder
                        .append("# ")
                        .append(breed)
                        .append("\n");
                if (CollectionUtils.isNotEmpty(subBreeds)) {
                    subBreeds.forEach(subBreed -> builder
                            .append("  > ")
                            .append(subBreed)
                            .append("\n"));
                }
            });
            String list = CommonUtils.trimTo(builder.toString().trim(), MessageEmbed.TEXT_MAX_LENGTH - 100);
            messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.breeds.list", list);
        } catch (HttpClientErrorException e) {
            messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.breeds.error");
        }
        return true;
    }

    private DogBreedsResponse getBreeds() {
        ResponseEntity<DogBreedsResponse> response = restTemplate.getForEntity(ENDPOINT_BREEDS, DogBreedsResponse.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException();
        }
        return response.getBody();
    }

    private boolean execute(MessageReceivedEvent message, String endPoint, Map<String, ?> uriVariables) {
        contextService.withContextAsync(message.getGuild(), () -> {
            try {
                message.getChannel().sendTyping().queue();
                ResponseEntity<DogResponse> response = restTemplate.getForEntity(endPoint, DogResponse.class, uriVariables);
                if (response.getStatusCode() != HttpStatus.OK
                        || response.getBody() == null
                        || !response.getBody().isSuccess()
                        || StringUtils.isEmpty(response.getBody().getMessage())) {
                    messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.error");
                    return;
                }
                EmbedBuilder builder = messageService.getBaseEmbed();
                builder.setImage(response.getBody().getMessage());
                builder.setColor(null);
                messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
            } catch (HttpClientErrorException e) {
                switch (e.getStatusCode()) {
                    case NOT_FOUND:
                        messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.error.notfound");
                        break;
                    default:
                        messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.error");
                        break;
                }
            }

        });
        return true;
    }
}
