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
package ru.juniperbot.module.misc.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.worker.command.model.AbstractCommandAsync;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.module.misc.DogApiService;
import ru.juniperbot.module.misc.model.dogapi.DogBreed;
import ru.juniperbot.module.misc.model.dogapi.DogImage;
import ru.juniperbot.module.misc.model.dogapi.DogMeasure;
import ru.juniperbot.module.misc.model.dogapi.DogSearchQuery;

import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@DiscordCommand(key = "discord.command.dog.key",
        description = "discord.command.dog.desc",
        group = "discord.command.group.fun",
        priority = 16)
public class DogCommand extends AbstractCommandAsync {

    private final Deque<DogImage> images = new ConcurrentLinkedDeque<>();

    @Autowired
    private DogApiService dogApiService;

    @Override
    public void doCommandAsync(GuildMessageReceivedEvent message, BotContext context, String query) {
        try {
            DogImage image;
            synchronized (images) {
                if (images.isEmpty()) {
                    images.addAll(dogApiService.search(DogSearchQuery.builder()
                            .limit(25)
                            .order(DogSearchQuery.Order.RANDOM)
                            .build()));
                }
                image = images.poll();
            }

            if (image != null) {
                sendImage(message, image);
                return;
            }
        } catch (Exception e) {
            log.warn("Could not get dog", e);
        }
        messageService.onEmbedMessage(message.getChannel(), "discord.command.dog.error");
    }

    private void sendImage(GuildMessageReceivedEvent event, DogImage image) {
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setImage(image.getUrl());
        if (CollectionUtils.isNotEmpty(image.getBreeds())) {
            DogBreed breed = image.getBreeds().get(0);
            if (StringUtils.isNotEmpty(breed.getName())) {
                builder.addField(messageService.getMessage("discord.command.dog.breed.title"), breed.getName(), true);
            }
            if (StringUtils.isNotEmpty(breed.getBreedGroup())) {
                builder.addField(messageService.getMessage("discord.command.dog.breedGroup.title"), breed.getBreedGroup(), true);
            }
            if (StringUtils.isNotEmpty(breed.getBredFor())) {
                builder.addField(messageService.getMessage("discord.command.dog.bredFor.title"), breed.getBredFor(), true);
            }
            if (StringUtils.isNotEmpty(breed.getTemperament())) {
                builder.addField(messageService.getMessage("discord.command.dog.temperament.title"), breed.getTemperament(), true);
            }
            String weight = getMeasure(breed.getWeight());
            if (StringUtils.isNotEmpty(weight)) {
                builder.addField(messageService.getMessage("discord.command.dog.weight.title"), weight, true);
            }
            String height = getMeasure(breed.getHeight());
            if (StringUtils.isNotEmpty(height)) {
                builder.addField(messageService.getMessage("discord.command.dog.height.title"), height, true);
            }
        }
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
    }

    private String getMeasure(DogMeasure measure) {
        if (measure == null) {
            return null;
        }
        return Locale.US.equals(contextService.getLocale()) ? measure.getImperial() : measure.getMetric();
    }
}
