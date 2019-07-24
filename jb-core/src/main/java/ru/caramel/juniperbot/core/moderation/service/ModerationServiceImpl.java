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
package ru.caramel.juniperbot.core.moderation.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.GuildController;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.audit.model.AuditActionBuilder;
import ru.caramel.juniperbot.core.audit.model.AuditActionType;
import ru.caramel.juniperbot.core.audit.service.ActionsHolderService;
import ru.caramel.juniperbot.core.common.persistence.LocalMember;
import ru.caramel.juniperbot.core.common.service.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.core.common.service.MemberService;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.moderation.model.ModerationActionRequest;
import ru.caramel.juniperbot.core.moderation.model.ModerationActionType;
import ru.caramel.juniperbot.core.moderation.persistence.*;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.DiscordUtils;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.caramel.juniperbot.core.audit.provider.MemberWarnAuditForwardProvider.*;

@Service
public class ModerationServiceImpl
        extends AbstractDomainServiceImpl<ModerationConfig, ModerationConfigRepository>
        implements ModerationService {

    private final static String COLOR_ROLE_NAME = "JB-CLR-";

    @Autowired
    private MemberWarningRepository warningRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ActionsHolderService actionsHolderService;

    @Autowired
    private MuteService muteService;

    private Cache<String, String> lastActionCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public ModerationServiceImpl(@Autowired ModerationConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected ModerationConfig createNew(long guildId) {
        ModerationConfig config = new ModerationConfig(guildId);
        config.setCoolDownIgnored(true);
        config.setActions(new ArrayList<>());
        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModerator(Member member) {
        if (member == null) {
            return false;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner()) {
            return true;
        }
        ModerationConfig config = get(member.getGuild());
        return config != null && CollectionUtils.isNotEmpty(config.getRoles())
                && member.getRoles().stream().anyMatch(e -> config.getRoles().contains(e.getIdLong()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublicColor(long guildId) {
        ModerationConfig config = getByGuildId(guildId);
        return config != null && config.isPublicColors();
    }

    @Override
    public boolean setColor(Member member, String color) {
        Role role = null;
        Guild guild = member.getGuild();
        Member self = guild.getSelfMember();

        GuildController controller = member.getGuild().getController();

        if (StringUtils.isNotEmpty(color)) {
            String roleName = COLOR_ROLE_NAME + color;
            List<Role> roles = member.getGuild().getRolesByName(roleName, false);
            role = roles.stream().filter(self::canInteract).findFirst().orElse(null);
            if (role == null) {
                role = controller
                        .createRole()
                        .setColor(CommonUtils.hex2Rgb(color))
                        .setMentionable(false)
                        .setName(roleName)
                        .complete();

                Role highestRole = DiscordUtils.getHighestRole(self, Permission.MANAGE_ROLES);
                if (highestRole != null) {
                    controller.modifyRolePositions()
                            .selectPosition(role)
                            .moveTo(highestRole.getPosition() - 1)
                            .complete();
                }
            }

            if (!self.canInteract(role)) {
                return false;
            }
        }

        if (role == null || !member.getRoles().contains(role)) {
            List<Role> roleList = member.getRoles().stream()
                    .filter(e -> e.getName().startsWith(COLOR_ROLE_NAME))
                    .filter(self::canInteract)
                    .collect(Collectors.toList());
            if (role != null) {
                if (CollectionUtils.isEmpty(roleList)) {
                    controller.addRolesToMember(member, role).complete();
                } else {
                    controller.modifyMemberRoles(member, Collections.singleton(role), roleList).complete();
                }
            } else {
                controller.removeRolesFromMember(member, roleList).complete();
            }
        }
        // remove unused color roles
        Set<Role> userRoles = new LinkedHashSet<>();
        if (role != null) {
            userRoles.add(role);
        }
        guild.getMembers().forEach(m -> userRoles.addAll(m.getRoles()));
        guild.getRoles().stream()
                .filter(e -> e.getName().startsWith(COLOR_ROLE_NAME) && !userRoles.contains(e) && self.canInteract(e))
                .forEach(e -> e.delete().queue());
        return true;
    }

    @Override
    @Transactional
    public boolean performAction(ModerationActionRequest request) {
        Member self = request.getGuild().getSelfMember();
        if (request.getModerator() != null) {
            lastActionCache.put(DiscordUtils.getMemberKey(request.getViolator()),
                    request.getModerator().getUser().getId());
        }
        switch (request.getType()) {
            case MUTE:
                return muteService.mute(request);
            case BAN:
                if (!self.hasPermission(Permission.BAN_MEMBERS) || !self.canInteract(request.getViolator())) {
                    return false;
                }
                notifyUserAction(e -> {
                    int delDays = request.getDuration() != null ? request.getDuration() : 0;
                    e.getGuild().getController().ban(e, delDays, request.getReason()).queue();
                }, request.getViolator(), "discord.command.mod.action.message.ban", request.getReason());
                return true;
            case KICK:
                if (!self.hasPermission(Permission.KICK_MEMBERS) || !self.canInteract(request.getViolator())) {
                    return false;
                }
                AuditActionBuilder actionBuilder = getAuditService()
                        .log(self.getGuild(), AuditActionType.MEMBER_KICK)
                        .withUser(request.getModerator())
                        .withTargetUser(request.getViolator())
                        .withAttribute(REASON_ATTR, request.getReason());

                notifyUserAction(e -> {
                    actionBuilder.save();
                    actionsHolderService.setLeaveNotified(e.getGuild().getIdLong(), e.getUser().getIdLong());
                    e.getGuild().getController().kick(e, request.getReason()).queue();
                }, request.getViolator(), "discord.command.mod.action.message.kick", request.getReason());
                return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean warn(Member author, Member member, String reason) {
        long guildId = member.getGuild().getIdLong();
        ModerationConfig moderationConfig = getOrCreate(member.getGuild());
        LocalMember authorLocal = memberService.getOrCreate(author);
        LocalMember memberLocal = memberService.getOrCreate(member);

        long count = warningRepository.countActiveByViolator(guildId, memberLocal);
        boolean exceed = count >= moderationConfig.getMaxWarnings() - 1;
        MemberWarning warning = new MemberWarning(guildId, authorLocal, memberLocal, reason);

        getAuditService().log(guildId, AuditActionType.MEMBER_WARN)
                .withUser(author)
                .withTargetUser(memberLocal)
                .withAttribute(REASON_ATTR, reason)
                .withAttribute(COUNT_ATTR, count + 1)
                .withAttribute(MAX_ATTR, moderationConfig.getMaxWarnings())
                .save();

        if (exceed) {
            reason = messageService.getMessage("discord.command.mod.warn.exceeded", count + 1);

            var builder = ModerationActionRequest.builder()
                    .moderator(author)
                    .violator(member)
                    .reason(reason);

            switch (moderationConfig.getWarnExceedAction()) {
                case BAN:
                    builder.type(ModerationActionType.BAN);
                    break;
                case KICK:
                    builder.type(ModerationActionType.KICK);
                    break;
                case MUTE:
                    builder.type(ModerationActionType.MUTE)
                            .global(true)
                            .duration(moderationConfig.getMuteCount());
                    break;
            }
            if (performAction(builder.build())) {
                warningRepository.flushWarnings(guildId, memberLocal);
                warning.setActive(false);
            }
        } else {
            notifyUserAction(e -> {}, member, "discord.command.mod.action.message.warn", reason, count + 1,
                    moderationConfig.getMaxWarnings());
        }
        warningRepository.save(warning);
        return exceed;
    }

    @Override
    @Transactional
    public List<MemberWarning> getWarnings(Member member) {
        LocalMember localMember = memberService.getOrCreate(member);
        return warningRepository.findActiveByViolator(member.getGuild().getIdLong(), localMember);
    }

    @Override
    @Transactional
    public long warnCount(Member member) {
        LocalMember memberLocal = memberService.getOrCreate(member);
        return warningRepository.countActiveByViolator(member.getGuild().getIdLong(), memberLocal);
    }

    @Override
    @Transactional
    public void removeWarn(@NonNull MemberWarning warning) {
        warning.setActive(false);
        warningRepository.save(warning);
    }

    @Override
    public Member getLastActionModerator(@NonNull Member violator) {
        String moderatorUserId = lastActionCache.getIfPresent(DiscordUtils.getMemberKey(violator));
        return moderatorUserId != null ? violator.getGuild().getMemberById(moderatorUserId) : null;
    }

    @Override
    public Member getLastActionModerator(@NonNull Guild guild, @NonNull User violator) {
        String moderatorUserId = lastActionCache.getIfPresent(DiscordUtils.getMemberKey(guild, violator));
        return moderatorUserId != null ? guild.getMemberById(moderatorUserId) : null;
    }

    private void notifyUserAction(Consumer<Member> consumer, Member member, String code, String reason, Object... objects) {
        if (StringUtils.isEmpty(reason)) {
            code += ".noReason";
        }
        if (member.getUser().isBot()) {
            return; // do not notify bots
        }
        String finalCode = code;
        try {
            Object[] args = new Object[]{member.getGuild().getName()};
            if (ArrayUtils.isNotEmpty(objects)) {
                args = ArrayUtils.addAll(args, objects);
            }
            if (StringUtils.isNotEmpty(reason)) {
                args = ArrayUtils.add(args, reason);
            }
            String message = messageService.getMessage(finalCode, args);

            JDA jda = member.getGuild().getJDA();
            long guildId = member.getGuild().getIdLong();
            long userId = member.getUser().getIdLong();

            member.getUser().openPrivateChannel().queue(e -> {
                contextService.withContext(guildId, () -> {
                    e.sendMessage(message).queue(t -> {
                        Guild guild = jda.getGuildById(guildId);
                        consumer.accept(guild != null ? guild.getMemberById(userId) : null);
                    }, t -> {
                        Guild guild = jda.getGuildById(guildId);
                        consumer.accept(guild != null ? guild.getMemberById(userId) : null);
                    });
                });
            }, t -> {
                Guild guild = jda.getGuildById(guildId);
                consumer.accept(guild != null ? guild.getMemberById(userId) : null);
            });
        } catch (Exception e) {
            consumer.accept(member);
        }
    }

    @Override
    protected Class<ModerationConfig> getDomainClass() {
        return ModerationConfig.class;
    }
}
