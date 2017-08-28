package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.configuration.DiscordConfig;
import ru.caramel.juniperbot.service.MessageService;

import java.awt.*;
import java.util.function.Function;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private DiscordConfig discordConfig;

    @Override
    public EmbedBuilder getBaseEmbed() {
        return new EmbedBuilder().setColor(discordConfig.getAccentColor());
    }

    @Override
    public void onError(MessageChannel sourceChannel, String error) {
        onError(sourceChannel, "Ошибка", error);
    }

    @Override
    public void onError(MessageChannel sourceChannel, String title, String error) {
        sendMessageSilent(sourceChannel::sendMessage, getBaseEmbed()
                .setTitle(title, null)
                .setColor(Color.RED)
                .setDescription(error).build());
    }

    @Override
    public void onMessage(MessageChannel sourceChannel, String message) {
        onMessage(sourceChannel, null, message);
    }


    @Override
    public void onMessage(MessageChannel sourceChannel, String title, String message) {
        if (StringUtils.isEmpty(title)) {
            sendMessageSilent(sourceChannel::sendMessage, message);
            return;
        }
        sendMessageSilent(sourceChannel::sendMessage, getBaseEmbed()
                .setTitle(title, null)
                .setDescription(message).build());
    }

    @Override
    public <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed) {
        try {
            action.apply(embed).queue();
        } catch (PermissionException e) {
            LOGGER.warn("No permission to message", e);
        }
    }

}
