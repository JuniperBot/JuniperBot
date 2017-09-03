package ru.caramel.juniperbot.service.listeners;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.integration.discord.DiscordEventListener;
import ru.caramel.juniperbot.service.ConfigService;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.service.PermissionsService;

@Component
public class GuildJoinListener extends DiscordEventListener {

    @Autowired
    private ConfigService configService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private PermissionsService permissionsService;

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        boolean exists = configService.exists(guild.getIdLong());
        for (TextChannel channel : guild.getTextChannels()) {
            if (permissionsService.checkPermission(channel, Permission.MESSAGE_WRITE)) {
                messageService.onMessage(channel, exists ? "discord.welcome.again" : "discord.welcome");
                break;
            }
        }
    }
}