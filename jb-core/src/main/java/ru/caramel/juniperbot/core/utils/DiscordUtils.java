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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordUtils {

    public final static String EVERYONE = "@everyone";

    private static final Pattern MEMBER_MENTION_PATTERN = Pattern.compile("@(.*?)#([0-9]{4})");

    private static final Pattern EMOTE_PATTERN = Pattern.compile(":([^:]*?)(~\\d+)?:");

    private static final Permission[] CHANNEL_WRITE_PERMISSIONS = new Permission[]{
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
                searchList[index] = "#" + channel.getName();
                replacementList[index] = channel.getAsMention();
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
                    searchList[index] = "@" + formatUser(member.getUser());
                    replacementList[index] = member.getAsMention();
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
                        searchList[index] = "@" + channel.getName();
                        replacementList[index] = channel.getAsMention();
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
                if (StringUtils.isEmpty(replacement)) {
                    continue;
                }
                List<Emote> emotes = guild.getEmotesByName(replacement, false);
                emotes.sort(Comparator.comparing(Emote::getCreationTime));
                if (emotes.isEmpty()) {
                    continue;
                }
                Emote emote = emotes.get(0);

                if (StringUtils.isNotEmpty(m.group(2))) {
                    replacement += m.group(2);
                    try {
                        int num = Integer.parseInt(m.group(2).substring(1));
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

    public static String maskPublicMentions(String value) {
        if (value == null) {
            return null;
        }
        value = value.replace("@everyone", "@\u2063everyone");
        value = value.replace("@here", "@\u2063here");
        return value;
    }

    public static String formatUser(User user) {
        return String.format("%s#%s", user.getName(), user.getDiscriminator());
    }

    public static String getUrl(String url) {
        if (StringUtils.isEmpty(url) || url.length() > MessageEmbed.URL_MAX_LENGTH) {
            return null;
        }
        if (EmbedBuilder.URL_PATTERN.matcher(url).matches()) {
            return url;
        }
        try {
            String result = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8);
            if (EmbedBuilder.URL_PATTERN.matcher(result).matches()) {
                return result;
            }
        } catch (Exception e) {
            // nah I don't care
        }
        return null;
    }

    public static Role getHighestRole(Member member, Permission... permission) {
        if (member == null || CollectionUtils.isEmpty(member.getRoles())) {
            return null;
        }
        return member.getRoles().stream()
                .sorted(Comparator.comparingInt(Role::getPosition).reversed())
                .filter(e -> permission == null || permission.length == 0 || e.hasPermission(permission))
                .findFirst().orElse(null);
    }

    public static Icon createIcon(String iconUrl) {
        if (UrlValidator.getInstance().isValid(iconUrl)) {
            try {
                return Icon.from(new URL(iconUrl).openStream());
            } catch (Exception e) {
                // fall down
            }
        }
        return null;
    }

    public static MessageChannel getChannel(JDA jda, ChannelType type, long channelId) {
        switch (type) {
            case TEXT:
                return jda.getTextChannelById(channelId);
            case PRIVATE:
                return jda.getPrivateChannelById(channelId);
            default:
                return null;
        }
    }

    public static String getMemberKey(@NonNull Member member) {
        return getMemberKey(member.getGuild(), member.getUser());
    }

    public static String getMemberKey(@NonNull Guild guild, @NonNull User user) {
        return String.format("%s:%s", guild.getId(), user.getId());
    }
}
