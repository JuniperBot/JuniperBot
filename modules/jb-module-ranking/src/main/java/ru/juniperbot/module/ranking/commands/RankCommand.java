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
package ru.juniperbot.module.ranking.commands;

import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.module.render.model.ReportType;
import ru.juniperbot.module.render.service.ImagingService;
import ru.juniperbot.module.render.service.JasperReportsService;
import ru.juniperbot.module.render.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DiscordCommand(
        key = "discord.command.rank.key",
        description = "discord.command.rank.desc",
        group = "discord.command.group.ranking",
        priority = 202)
public class RankCommand extends RankingCommand {

    @Autowired
    private JasperReportsService reportsService;

    @Autowired
    private ImagingService imagingService;

    @Setter
    private boolean cardEnabled = true;

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        contextService.queue(message.getGuild(), message.getChannel().sendTyping(), e -> {
            Member member = message.getMember();
            if (CollectionUtils.isNotEmpty(message.getMessage().getMentionedUsers())) {
                member = message.getGuild().getMember(message.getMessage().getMentionedUsers().get(0));
            }
            if (member == null) {
                return;
            }
            RankingInfo info = rankingService.getRankingInfo(member);

            Member self = message.getGuild().getSelfMember();
            if (cardEnabled
                    && self.hasPermission(message.getChannel(), Permission.MESSAGE_ATTACH_FILES)
                    && sendCard(message.getChannel(), member, info)) {
                return;
            }

            EmbedBuilder builder = messageService.getBaseEmbed(true);
            addFields(builder, info, member.getGuild());

            long desiredPage = (info.getRank() / 50) + 1;
            String url = String.format("https://juniper.bot/ranking/%s?page=%s#%s", member.getGuild().getId(),
                    desiredPage, member.getUser().getId());
            builder.setAuthor(member.getEffectiveName(), url, member.getUser().getAvatarUrl());
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        });
        return true;
    }

    private boolean sendCard(TextChannel channel, Member member, RankingInfo info) {
        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("name", ""); // it fails on font fallback so we have to render it on our own
        templateMap.put("avatarImage", imagingService.getAvatarWithStatus(member));
        templateMap.put("backgroundImage", imagingService.getResourceImage("ranking-card-background.png"));
        templateMap.put("percent", info.getPct());
        templateMap.put("remainingExp", info.getRemainingExp());
        templateMap.put("levelExp", info.getLevelExp());
        templateMap.put("totalExp", info.getTotalExp());
        templateMap.put("level", info.getLevel());
        templateMap.put("rank", info.getRank());
        templateMap.put("cookies", info.getCookies());
        if (info.getVoiceActivity() > 0) {
            templateMap.put("voiceActivity", CommonUtils.formatDuration(info.getVoiceActivity()));
        }
        templateMap.put("rankText", messageService.getMessage("discord.command.rank.info.rank.short.title"));
        templateMap.put("levelText", messageService.getMessage("discord.command.rank.info.lvl.short.title"));

        BufferedImage cardImage = reportsService.generateImage(ReportType.RANKING_CARD, templateMap);
        if (cardImage == null) {
            return false;
        }

        Graphics2D g2d = cardImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        List<Font> fontVariants = reportsService.loadFontVariants(Font.PLAIN, 40, "Roboto Light");
        g2d.drawString(ImageUtils.createFallbackString(member.getEffectiveName(), fontVariants).getIterator(), 200, 63);
        g2d.dispose();

        byte[] cardBytes = ImageUtils.getImageBytes(cardImage, "png");
        if (cardBytes == null) {
            return false;
        }
        channel.sendFile(cardBytes, "ranking-card.png").queue();
        return true;
    }

    public void addFields(EmbedBuilder builder, RankingInfo info, Guild guild) {
        RankingConfig config = rankingConfigService.get(guild);
        if (config == null) {
            return;
        }
        long totalMembers = rankingConfigService.countRankings(guild.getIdLong());
        builder.addField(messageService.getMessage("discord.command.rank.info.rank.title"),
                String.format("# %d/%d", info.getRank(), totalMembers), true);
        builder.addField(messageService.getMessage("discord.command.rank.info.lvl.title"),
                String.valueOf(info.getLevel()), true);
        builder.addField(messageService.getMessage("discord.command.rank.info.exp.title"),
                messageService.getMessage("discord.command.rank.info.exp.format",
                        info.getRemainingExp(), info.getLevelExp(), info.getTotalExp()), true);
        if (config.isCookieEnabled()) {
            builder.addField(messageService.getMessage("discord.command.rank.info.cookies.title"),
                    String.format("%d \uD83C\uDF6A", info.getCookies()), true);
        }
        if (info.getVoiceActivity() > 0) {
            builder.addField(messageService.getMessage("discord.command.rank.info.voiceActivity.title"),
                    CommonUtils.formatDuration(info.getVoiceActivity()), true);
        }
    }
}
