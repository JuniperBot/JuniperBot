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
package ru.juniperbot.common.worker.command.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.GatewayService;
import ru.juniperbot.common.worker.command.service.InternalCommandsService;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.feature.service.FeatureSetService;
import ru.juniperbot.common.worker.message.service.MessageService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;
import ru.juniperbot.common.worker.shared.service.DiscordService;

public abstract class AbstractCommand implements Command {

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected CommonProperties commonProperties;

    @Autowired
    protected InternalCommandsService commandsService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected FeatureSetService featureSetService;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected DiscordEntityAccessor entityAccessor;

    @Autowired
    protected GatewayService gatewayService;

    private DiscordCommand annotation;

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return true;
    }

    protected boolean ok(GuildMessageReceivedEvent message) {
        commandsService.resultEmotion(message, "✅", null);
        return true;
    }

    protected boolean fail(GuildMessageReceivedEvent message) {
        commandsService.resultEmotion(message, "❌", null);
        return false;
    }

    protected boolean ok(GuildMessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "✅", messageCode, args);
        return true;
    }

    protected boolean fail(GuildMessageReceivedEvent message, String messageCode, Object... args) {
        commandsService.resultEmotion(message, "❌", messageCode, args);
        return false;
    }

    @Override
    public DiscordCommand getAnnotation() {
        if (annotation == null) {
            annotation = getClass().getDeclaredAnnotation(DiscordCommand.class);
        }
        return annotation;
    }
}
