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
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordUtils {

    private static final Pattern MEMBER_MENTION_PATTERN = Pattern.compile("@(.*?)#([0-9]{4})");

    private static final Pattern EMOTE_PATTERN = Pattern.compile(":([^:]*?)~?(\\d+)?:");

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

    public static Member findMember(@NonNull Guild guild, String name, String discriminator) {
        return guild.getMembersByName(name, true)
                .stream()
                .filter(m -> m.getUser() != null && m.getUser().getDiscriminator().equals(discriminator))
                .findFirst()
                .orElse(null);

    }

    public static String replaceReferences(String content, @NonNull Guild guild) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }

        int i;
        String[] searchList;
        String[] replacementList;

        if (content.contains("#")) {
            List<TextChannel> channels = guild.getTextChannels();
            i = 0;
            searchList = new String[channels.size()];
            replacementList = new String[channels.size()];
            for (TextChannel channel : channels) {
                int index = i++;
                searchList[index] =  "#" + channel.getName();
                replacementList[index] =  channel.getAsMention();
            }
            content = StringUtils.replaceEach(content, searchList, replacementList);
        }

        if (content.contains("@")) {
            // replace member mentions
            Matcher m = MEMBER_MENTION_PATTERN.matcher(content);
            Set<Member> mentioned = new HashSet<>();
            while (m.find()) {
                Member member = findMember(guild, m.group(1), m.group(2));
                if (member != null) {
                    mentioned.add(member);
                }
            }

            if (CollectionUtils.isNotEmpty(mentioned)) {
                i = 0;
                searchList = new String[mentioned.size()];
                replacementList = new String[mentioned.size()];
                for (Member member : mentioned) {
                    int index = i++;
                    User user = member.getUser();
                    searchList[index] =  String.format("@%s#%s", user.getName(), user.getDiscriminator());
                    replacementList[index] =  member.getAsMention();
                }
                content = StringUtils.replaceEach(content, searchList, replacementList);
            }

            if (content.contains("@")) {
                List<Role> roles = guild.getRoles();
                if (CollectionUtils.isNotEmpty(roles)) {
                    i = 0;
                    searchList = new String[roles.size()];
                    replacementList = new String[roles.size()];
                    for (Role channel : roles) {
                        int index = i++;
                        searchList[index] =  "@" + channel.getName();
                        replacementList[index] =  channel.getAsMention();
                    }
                    content = StringUtils.replaceEach(content, searchList, replacementList);
                }
            }
        }

        if (content.contains(":")) {
            Matcher m = EMOTE_PATTERN.matcher(content);

            List<String> emotePlaceholders = new LinkedList<>();
            List<String> emoteMentions = new LinkedList<>();

            while (m.find()) {
                String replacement = m.group(1);
                List<Emote> emotes = guild.getEmotesByName(replacement, false);
                emotes.sort(Comparator.comparing(Emote::getCreationTime));
                if (emotes.isEmpty()) {
                    continue;
                }
                Emote emote = emotes.get(0);

                if (StringUtils.isNumeric(m.group(2))) {
                    replacement += "~" + m.group(2);
                    try {
                        int num = Integer.parseInt(m.group(2));
                        if (num < emotes.size()) {
                            emote = emotes.get(num);
                            emotePlaceholders.add(0, ":" + replacement + ":");
                            emoteMentions.add(0, emote.getAsMention());
                        }
                    } catch (NumberFormatException e) {
                        // fall down
                    }
                    continue;
                }
                emotePlaceholders.add(":" + replacement + ":");
                emoteMentions.add(emote.getAsMention());
            }

            if (CollectionUtils.isNotEmpty(emotePlaceholders)) {
                searchList = new String[emotePlaceholders.size()];
                replacementList = new String[emoteMentions.size()];
                emotePlaceholders.toArray(searchList);
                emoteMentions.toArray(replacementList);
                content = StringUtils.replaceEach(content, searchList, replacementList);
            }
        }
        return content;
    }
}
