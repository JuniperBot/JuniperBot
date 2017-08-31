package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.function.Function;

public interface MessageService {

    EmbedBuilder getBaseEmbed();

    <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed);

    void onMessage(MessageChannel sourceChannel, String code, Object... args);

    void onMessage(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    void onError(MessageChannel sourceChannel, String code, Object... args);

    void onError(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    String getMessage(String code, Object... args);
}
