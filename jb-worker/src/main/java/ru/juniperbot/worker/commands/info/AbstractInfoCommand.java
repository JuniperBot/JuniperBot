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

import net.dv8tion.jda.api.entities.MessageEmbed;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import ru.juniperbot.common.worker.command.model.AbstractCommand;

public abstract class AbstractInfoCommand extends AbstractCommand {

    protected MessageEmbed.Field getDateField(long epochSecond, String nameKey, DateTimeFormatter formatter) {
        DateTime dateTime = new DateTime(epochSecond * 1000);
        return new MessageEmbed.Field(messageService.getMessage(nameKey),
                String.format("**%s**", formatter.print(dateTime)), true);
    }
}
