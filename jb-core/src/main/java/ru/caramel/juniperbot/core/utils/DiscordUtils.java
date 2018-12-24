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
package ru.caramel.juniperbot.core.utils;

import lombok.NonNull;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public final class DiscordUtils {

    private static final Permission[] CHANNEL_WRITE_PERMISSIONS = new Permission[] {
            Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE,
            Permission.MESSAGE_EMBED_LINKS
    };

    private DiscordUtils() {
        // helper class
    }

    public static TextChannel getDefaultWriteableChannel(@NonNull Guild guild) {
        Member self = guild.getSelfMember();
        TextChannel channel = guild.getDefaultChannel();
        if (channel != null && self.hasPermission(channel, CHANNEL_WRITE_PERMISSIONS)) {
            return channel;
        }
        for (TextChannel textChannel : guild.getTextChannels()) {
            if (self.hasPermission(textChannel, CHANNEL_WRITE_PERMISSIONS)) {
                return textChannel;
            }
        }
        return null;
    }
}
