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
package ru.juniperbot.worker.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.entity.Gulag;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.GulagService;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.utils.LocaleUtils;
import ru.juniperbot.common.worker.command.service.CoolDownService;
import ru.juniperbot.common.worker.event.DiscordEvent;
import ru.juniperbot.common.worker.event.listeners.DiscordEventListener;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.message.service.MessageService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;

import java.util.function.Function;

@DiscordEvent(priority = 0)
public class GuildListener extends DiscordEventListener {

    private static final String NEW_LINE = "\n" + EmbedBuilder.ZERO_WIDTH_SPACE;

    @Autowired
    private DiscordEntityAccessor entityAccessor;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private CoolDownService coolDownService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private GulagService gulagService;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();

        TextChannel channel = guild.getDefaultChannel();
        if (channel != null && !self.hasPermission(channel,
                Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
            channel = null;
        }

        if (channel == null) {
            for (TextChannel textChannel : guild.getTextChannels()) {
                if (self.hasPermission(textChannel,
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                    channel = textChannel;
                    break;
                }
            }
        }

        Gulag gulag = gulagService.getGulag(guild);
        if (gulag != null) {
            if (channel != null) {
                EmbedBuilder builder = messageService.getBaseEmbed();
                builder.setDescription(messageService.getMessage("discord.gulag.message"));
                builder.addField(messageService.getMessage("discord.gulag.reason"),
                        CommonUtils.trimTo(gulag.getReason(), MessageEmbed.VALUE_MAX_LENGTH), true);
                channel.sendMessage(builder.build()).queue(e -> guild.leave().queue(), e -> guild.leave().queue());
            } else {
                guild.leave().queue();
            }
            return;
        }

        GuildConfig config = entityAccessor.getOrCreate(event.getGuild());
        switch (event.getGuild().getRegion()) {
            case RUSSIA:
                config.setLocale(LocaleUtils.RU_LOCALE);
                config.setCommandLocale(LocaleUtils.RU_LOCALE);
                break;
            default:
                config.setLocale(LocaleUtils.DEFAULT_LOCALE);
                config.setCommandLocale(LocaleUtils.DEFAULT_LOCALE);
                break;
        }
        configService.save(config);
        contextService.initContext(event.getGuild()); // reinit context with updated locale
        sendWelcome(channel, event);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        coolDownService.clear(event.getGuild());
    }

    private void sendWelcome(TextChannel channel, GuildJoinEvent event) {
        Guild guild = event.getGuild();
        contextService.withContextAsync(guild, () -> {
            MessageEmbed embed = createWelcomeMessage(guild);
            if (channel != null) {
                channel.sendMessage(embed).queue();
            } else {
                try {
                    guild.getOwner().getUser().openPrivateChannel().queue(e -> {
                        if (e != null) {
                            e.sendMessage(embed).queue();
                        }
                    });
                } catch (Exception e) {
                    // oh, ok then, we don't care
                }
            }
        });
    }

    private MessageEmbed createWelcomeMessage(Guild guild) {
        Function<String, String> m = messageService::getMessage;
        String webPage = m.apply("about.support.page");
        String discordServer = m.apply("about.support.server");
        String githubPage = m.apply("about.support.github");
        EmbedBuilder builder = messageService.getBaseEmbed(true);
        builder.setDescription(messageService.getMessage("welcome.guild.message") + NEW_LINE);

        User self = guild.getJDA().getSelfUser();
        builder.setAuthor(self.getName(), webPage, self.getAvatarUrl());

        builder.addField(m.apply("welcome.fields.common.title"),
                m.apply("welcome.fields.common.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.music.title"),
                m.apply("welcome.fields.music.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.audit.title"),
                m.apply("welcome.fields.audit.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.subscriptions.title"),
                m.apply("welcome.fields.subscriptions.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.ranking.title"),
                m.apply("welcome.fields.ranking.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.welcome.title"),
                m.apply("welcome.fields.welcome.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.commands.title"),
                m.apply("welcome.fields.commands.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.custom.title"),
                m.apply("welcome.fields.custom.content") + NEW_LINE, false);
        builder.addField(m.apply("welcome.fields.fun.title"),
                m.apply("welcome.fields.fun.content") + NEW_LINE, false);

        builder.addField(m.apply("welcome.fields.support.title"),
                messageService.getMessage("welcome.fields.support.content", webPage, discordServer, githubPage),
                false);

        return builder.build();
    }
}