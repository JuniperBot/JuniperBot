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
package ru.caramel.juniperbot.module.steam.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.module.steam.model.details.*;
import ru.caramel.juniperbot.module.steam.persistence.entity.SteamApp;
import ru.caramel.juniperbot.module.steam.service.SteamService;

import java.util.List;

@DiscordCommand(key = "discord.command.steam.key",
        description = "discord.command.steam.desc",
        group = "discord.command.group.utility",
        priority = 15)
public class SteamCommand extends AbstractCommand {

    private static final String APP_PAGE =  "http://store.steampowered.com/app/";

    @Autowired
    private SteamService steamService;

    @Autowired
    private ContextService contextService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        if (StringUtils.isEmpty(query)) {
            messageService.onMessage(message.getChannel(), "discord.command.steam.enter");
            return true;
        }
        message.getChannel().sendTyping().submit();
        SteamApp app = null;
        if (StringUtils.isNumeric(query)) {
            app = steamService.findByAppId(Long.parseLong(query));
        }
        if (app == null) {
            app = steamService.findOne(query);
        }
        SteamAppDetails details = steamService.getDetails(app, contextService.getLocale());
        if (details == null) {
            messageService.onMessage(message.getChannel(), "discord.command.steam.noResults");
            return false;
        }

        String storeUrl = APP_PAGE + app.getAppId();

        EmbedBuilder builder = messageService.getBaseEmbed(true);
        if (StringUtils.isNotEmpty(details.getHeaderImage())) {
            builder.setThumbnail(details.getHeaderImage());
        }
        builder.setAuthor(details.getName(), storeUrl);
        builder.setDescription(details.getShortDescription());

        if (details.getReleaseDate() != null && details.getReleaseDate().isCorrect()) {
            builder.addField(getReleaseDate(details.getReleaseDate()));
        }
        if (details.getPrice() != null && details.getPrice().isCorrect()) {
            builder.addField(getPrice(details.getPrice()));
        }
        if (CollectionUtils.isNotEmpty(details.getDevelopers())) {
            builder.addField(getParts(details.getDevelopers(), "discord.command.steam.developers"));
        }
        if (CollectionUtils.isNotEmpty(details.getPublishers())) {
            builder.addField(getParts(details.getPublishers(), "discord.command.steam.publishers"));
        }
        if (details.getPlatforms() != null && details.getPlatforms().isCorrect()) {
            builder.addField(getPlatforms(details.getPlatforms()));
        }
        if (StringUtils.isNotEmpty(details.getWebSite())) {
            builder.addField(messageService.getMessage("discord.command.steam.website"), details.getWebSite(), true);
        }

        if (CollectionUtils.isNotEmpty(details.getScreenshots())) {
            for (SteamAppScreenshot screenshot : details.getScreenshots()) {
                if (StringUtils.isNotEmpty(screenshot.getFull())) {
                    builder.setImage(screenshot.getFull());
                    break;
                }
            }
        }
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    private MessageEmbed.Field getPlatforms(SteamAppPlatforms platforms) {
        StringBuilder builder = new StringBuilder();
        if (platforms.isWindows()) {
            builder.append("<:windows:404365879242129408> ");
        }
        if (platforms.isLinux()) {
            builder.append("<:steam_os:404365878621372417> ");
        }
        if (platforms.isMac()) {
            builder.append("<:mac:404365878373908481> ");
        }
        return new MessageEmbed.Field(messageService.getMessage("discord.command.steam.platforms"),
                builder.toString(), true);
    }

    private MessageEmbed.Field getParts(List<String> parts, String messageCode) {
        return new MessageEmbed.Field(messageService.getMessage(messageCode),
                StringUtils.join(parts, "\n"), true);
    }

    private MessageEmbed.Field getReleaseDate(SteamAppReleaseDate releaseDate) {
        StringBuilder builder = new StringBuilder();
        if (releaseDate.isComingSoon()) {
            builder.append(messageService.getMessage("discord.command.steam.releaseDate.comingSoon")).append(" ");
        }
        if (StringUtils.isNotEmpty(releaseDate.getDate())) {
            builder.append(releaseDate.getDate());
        }
        return new MessageEmbed.Field(messageService.getMessage("discord.command.steam.releaseDate"),
                builder.toString(), true);
    }

    private MessageEmbed.Field getPrice(SteamAppPrice price) {
        String value;
        try {
            CurrencyUnit unit = CurrencyUnit.of(price.getCurrency());
            double newPrice = price.getPrice() / Math.pow(10, unit.getDefaultFractionDigits());
            Money priceMoney = Money.of(unit, newPrice);

            MoneyFormatter formatter = null;
            if (CurrencyUnit.USD.equals(unit)) {
                formatter = new MoneyFormatterBuilder()
                        .appendCurrencySymbolLocalized()
                        .appendAmountLocalized()
                        .toFormatter(contextService.getLocale());
            } else {
                formatter = new MoneyFormatterBuilder()
                        .appendAmountLocalized()
                        .appendLiteral(" ")
                        .appendCurrencySymbolLocalized()
                        .toFormatter(contextService.getLocale());
            }
            value = formatter.print(priceMoney);
        } catch (Exception e) {
            double newPrice = price.getPrice() / 100;
            value = String.format("%f (%s)", newPrice, price.getCurrency());
        }
        return new MessageEmbed.Field(messageService.getMessage("discord.command.steam.price"), value, true);
    }
}
