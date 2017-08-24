package ru.caramel.juniperbot.service.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
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
    public void sendMessageSilent(Function<MessageEmbed, RestAction<Message>> action, MessageEmbed embed) {
        try {
            action.apply(embed).queue();
        } catch (PermissionException e) {
            LOGGER.warn("No permission to message", e);
        }
    }

}
