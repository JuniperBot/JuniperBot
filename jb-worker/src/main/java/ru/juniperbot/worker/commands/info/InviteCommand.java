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
package ru.juniperbot.worker.commands.info;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.juniperbot.common.model.AvatarType;
import ru.juniperbot.common.model.InviteInfo;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.shared.service.DiscordService;

import javax.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.invite.key",
        description = "discord.command.invite.desc",
        group = "discord.command.group.info",
        priority = 22)
public class InviteCommand extends ServerInfoCommand {

    private static final String INVITE_INFO_URL = "https://discordapp.com/api/invites/{code}";

    private static Pattern INVITE_CODE_PATTERN = Pattern.compile("^[a-z0-9-]+$", Pattern.CASE_INSENSITIVE);

    private RestTemplate restTemplate;

    @Autowired
    private DiscordService discordService;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate(CommonUtils.createRequestFactory());
    }

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        String code = extractCode(query);
        if (StringUtils.isBlank(code)) {
            messageService.onTempEmbedMessage(message.getChannel(), 10, "discord.command.invite.empty");
            return false;
        }

        contextService.queue(message.getGuild(), message.getChannel().sendTyping(), v -> {
            try {
                ResponseEntity<InviteInfo> response = restTemplate.getForEntity(INVITE_INFO_URL, InviteInfo.class, code);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    MessageEmbed embed = createInfoMessage(response.getBody(), context);
                    messageService.sendMessageSilent(message.getChannel()::sendMessage, embed);
                    return;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    messageService.onTempEmbedMessage(message.getChannel(), 10, "discord.command.invite.notFound");
                    return;
                }
            } catch (Exception e) {
                // fall down
            }
            messageService.onError(message.getChannel(), "discord.command.invite.title", "discord.command.invite.error");
        });
        return true;
    }

    private MessageEmbed createInfoMessage(InviteInfo inviteInfo, BotContext context) {
        EmbedBuilder builder = messageService.getBaseEmbed(false);

        if (inviteInfo.getGuild() != null) {
            InviteInfo.Guild guildInfo = inviteInfo.getGuild();
            String iconUrl = AvatarType.ICON.getUrl(guildInfo.getId(), guildInfo.getIcon());
            String inviteUrl = "https://discord.gg/" + (StringUtils.isNotEmpty(guildInfo.getVanityUrlCode()) ? guildInfo.getVanityUrlCode() : inviteInfo.getCode());
            builder.setTitle(String.format("%s (ID: %s)", guildInfo.getName(), guildInfo.getId()), inviteUrl);
            builder.setDescription(guildInfo.getDescription());
            builder.setThumbnail(iconUrl);

            Guild guild = discordService.getGuildById(Long.valueOf(guildInfo.getId()));
            if (guild != null) {
                builder.addField(getMemberListField(guild));
                builder.addField(getChannelListField(guild));
                builder.addField(getShard(guild));
                builder.addField(getVerificationLevel(guild.getVerificationLevel()));
                builder.addField(getRegion(guild));
                builder.addField(getOwner(guild));
                builder.addField(getCreatedAt(guild, context));
            } else {
                if (guildInfo.getVerificationLevel() != null) {
                    builder.addField(getVerificationLevel(Guild.VerificationLevel.fromKey(guildInfo.getVerificationLevel())));
                }
            }
        }

        if (inviteInfo.getChannel() != null) {
            InviteInfo.Channel channel = inviteInfo.getChannel();
            String info = String.format("`#%s` (ID: %s)", channel.getName(), channel.getId());
            builder.addField(messageService.getMessage("discord.command.invite.channel"), info, true);
        }

        if (inviteInfo.getInviter() != null) {
            InviteInfo.Inviter inviter = inviteInfo.getInviter();
            String avatarUrl = StringUtils.isNotEmpty(inviter.getAvatar()) ? AvatarType.AVATAR.getUrl(inviter.getId(), inviter.getAvatar()) : null;
            builder.setFooter(String.format("%s#%s (ID: %s)", inviter.getUsername(), inviter.getDiscriminator(), inviter.getId()), avatarUrl);
        }
        return builder.build();
    }

    private String extractCode(String value) {
        Matcher matcher = Message.INVITE_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = INVITE_CODE_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
}
