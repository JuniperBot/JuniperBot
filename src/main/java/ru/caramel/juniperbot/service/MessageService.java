package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.function.Function;

public interface MessageService {

    EmbedBuilder getBaseEmbed();

    <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed);

    void onMessage(MessageChannel sourceChannel, String message);

    void onMessage(MessageChannel sourceChannel, String title, String message);

    void onError(MessageChannel sourceChannel, String error);

    void onError(MessageChannel sourceChannel, String title, String error);
}
