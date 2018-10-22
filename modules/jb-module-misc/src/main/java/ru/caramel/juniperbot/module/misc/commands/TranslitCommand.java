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
package ru.caramel.juniperbot.module.misc.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.service.MessageService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@DiscordCommand(key = "discord.command.translit.key",
        description = "discord.command.translit.desc",
        group = "discord.command.group.utility",
        priority = 15)
public class TranslitCommand extends AbstractCommand {

    private final static Map<String, String> TRANSLIT_MAP = makeTranslitMap();

    private static Map<String, String> makeTranslitMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "а");
        map.put("b", "б");
        map.put("v", "в");
        map.put("g", "г");
        map.put("d", "д");
        map.put("e", "е");
        map.put("yo", "ё");
        map.put("zh", "ж");
        map.put("z", "з");
        map.put("i", "и");
        map.put("j", "й");
        map.put("k", "к");
        map.put("l", "л");
        map.put("m", "м");
        map.put("n", "н");
        map.put("o", "о");
        map.put("p", "п");
        map.put("r", "р");
        map.put("s", "с");
        map.put("t", "т");
        map.put("u", "у");
        map.put("f", "ф");
        map.put("h", "х");
        map.put("ts", "ц");
        map.put("ch", "ч");
        map.put("sh", "ш");
        map.put("`", "ъ");
        map.put("y", "у");
        map.put("'", "ь");
        map.put("yu", "ю");
        map.put("ya", "я");
        map.put("x", "кс");
        map.put("w", "в");
        map.put("q", "к");
        map.put("iy", "ий");
        return map;
    }

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        if (StringUtils.isEmpty(query)) {
            messageService.onError(message.getChannel(), "discord.command.translit.title", "discord.command.translit.empty");
            return false;
        }
        String userName = message.getChannelType().isGuild()
                ? message.getMember().getEffectiveName()
                : message.getAuthor().getName();
        messageService.sendMessageSilent(message.getChannel()::sendMessage, messageService.getMessage(
                "discord.command.translit.format", userName, untranslit(query)));
        return true;
    }

    private static String untranslit(String text) {
        Function<String, String> get = s -> {
            String result = TRANSLIT_MAP.get(s.toLowerCase());
            return result == null ? "" : (Character.isUpperCase(s.charAt(0)) ? (result.charAt(0) + "").toUpperCase() +
                    (result.length() > 1 ? result.substring(1) : "") : result);
        };

        int len = text.length();
        if (len == 0) {
            return text;
        }
        if (len == 1) {
            return get.apply(text);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; ) {
            // get next 2 symbols
            String toTranslate = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
            // trying to translate
            String translated = get.apply(toTranslate);
            // if these 2 symbols are not connected try to translate one by one
            if (StringUtils.isEmpty(translated)) {
                translated = get.apply(toTranslate.charAt(0) + "");
                sb.append(StringUtils.isEmpty(translated) ? toTranslate.charAt(0) : translated);
                i++;
            } else {
                sb.append(StringUtils.isEmpty(translated) ? toTranslate : translated);
                i += 2;
            }
        }
        return sb.toString();
    }

}
