package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.function.Function;

public interface MessageService {

    EmbedBuilder getBaseEmbed();

    void sendMessageSilent(Function<MessageEmbed, RestAction<Message>> action, MessageEmbed embed);

    void onError(MessageChannel sourceChannel, String error);

    void onError(MessageChannel sourceChannel, String title, String error);
}
