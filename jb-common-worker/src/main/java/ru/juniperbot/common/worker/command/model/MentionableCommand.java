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

import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.service.UserService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public abstract class MentionableCommand extends AbstractCommand {

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(<@!?([0-9]*)>|([0-9]*))(.*)");

    private boolean authorAllowed;

    private boolean membersOnly;

    protected MentionableCommand(boolean authorAllowed, boolean membersOnly) {
        this.authorAllowed = authorAllowed;
        this.membersOnly = membersOnly;
    }

    @Autowired
    protected MemberService memberService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DiscordEntityAccessor entityAccessor;

    @Override
    public final boolean doCommand(GuildMessageReceivedEvent event, BotContext context, String content) {
        if (!authorAllowed && StringUtils.isEmpty(content)) {
            showHelp(event, context);
            return false;
        }
        Matcher matcher = MESSAGE_PATTERN.matcher(content);
        if (!matcher.find()) {
            showHelp(event, context);
            return false;
        }
        content = matcher.group(4);

        MemberReference reference = new MemberReference();
        String id = matcher.group(2);
        if (StringUtils.isEmpty(id)) {
            id = matcher.group(3);
        }
        if (StringUtils.isEmpty(id)) {
            if (!authorAllowed) {
                showHelp(event, context);
                return false;
            }
            id = event.getAuthor().getId();
            reference.setUser(event.getAuthor());
            reference.setMember(event.getMember());
        } else {
            reference.setUser(discordService.getUserById(id));
            reference.setMember(event.getGuild().getMemberById(id));
        }

        reference.setId(id);
        if (reference.getUser() != null) {
            reference.setLocalUser(entityAccessor.getOrCreate(reference.getUser()));
        } else {
            reference.setLocalUser(userService.getById(id));
        }
        if (reference.getMember() != null) {
            reference.setLocalMember(entityAccessor.getOrCreate(reference.getMember()));
        } else {
            reference.setLocalMember(memberService.get(event.getGuild().getIdLong(), id));
        }

        if (membersOnly && reference.getLocalMember() == null && reference.getMember() == null) {
            showHelp(event, context);
            return false;
        }
        return doCommand(reference, event, context, content);
    }

    protected abstract boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String content);

    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        fail(event);
    }
}
