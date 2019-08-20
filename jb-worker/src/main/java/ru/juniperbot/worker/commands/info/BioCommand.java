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
package ru.juniperbot.worker.commands.info;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.persistence.entity.MemberBio;
import ru.juniperbot.common.persistence.repository.MemberBioRepository;

@DiscordCommand(key = BioCommand.KEY,
        description = "discord.command.bio.desc",
        group = "discord.command.group.info",
        priority = 15)
public class BioCommand extends AbstractInfoCommand {

    public static final String KEY = "discord.command.bio.key";

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberBioRepository bioRepository;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        LocalMember localMember = entityAccessor.getOrCreate(message.getMember());
        MemberBio bio = bioRepository.findByMember(localMember);
        if (bio == null) {
            bio = new MemberBio();
            bio.setMember(localMember);
        }

        String userCommand = messageService.getMessageByLocale("discord.command.user.key",
                context.getCommandLocale());
        String bioCommand = messageService.getMessageByLocale("discord.command.bio.key",
                context.getCommandLocale());

        if (StringUtils.isEmpty(query)) {
            EmbedBuilder builder = messageService.getBaseEmbed(true);
            if (StringUtils.isNotEmpty(bio.getBio())) {
                builder.appendDescription(bio.getBio()).appendDescription("\n\n--------\n");
            }
            builder.appendDescription(messageService.getMessage("discord.command.bio.info",
                    context.getConfig().getPrefix(), bioCommand, userCommand));
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
            return true;
        }
        bio.setBio("-".equals(query) ? null : CommonUtils.trimTo(query.trim(), MessageEmbed.TEXT_MAX_LENGTH - 500));
        bioRepository.save(bio);

        String updatedMsg = messageService.getMessage("discord.command.bio.updated",
                context.getConfig().getPrefix(), userCommand);
        return ok(message, updatedMsg);
    }
}
