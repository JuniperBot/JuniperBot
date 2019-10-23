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
package ru.juniperbot.common.worker.shared.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.persistence.repository.LocalMemberRepository;
import ru.juniperbot.common.persistence.repository.LocalUserRepository;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.service.UserService;

import java.util.Objects;

@Service
public class DiscordEntityAccessorImpl implements DiscordEntityAccessor {

    private final Object $userLock = new Object[0];

    private final Object $memberLock = new Object[0];

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Autowired
    protected LocalUserRepository userRepository;

    @Autowired
    protected LocalMemberRepository memberRepository;

    @Override
    @Transactional
    public GuildConfig getOrCreate(Guild guild) {
        GuildConfig config = configService.getOrCreate(guild.getIdLong());
        return updateIfRequred(guild, config);
    }

    @Override
    @Transactional
    public LocalUser getOrCreate(User user) {
        if (!userService.isApplicable(user)) {
            return null;
        }
        LocalUser localUser = userService.get(user);
        if (localUser == null) {
            synchronized ($userLock) {
                localUser = userService.get(user);
                if (localUser == null) {
                    localUser = new LocalUser();
                    localUser.setUserId(user.getId());
                    updateIfRequired(user, localUser);
                    userRepository.flush();
                    return localUser;
                }
            }
        }
        return updateIfRequired(user, localUser);
    }

    @Override
    @Transactional
    public LocalMember getOrCreate(Member member) {
        if (!memberService.isApplicable(member)) {
            return null;
        }
        LocalMember localMember = memberService.get(member);
        if (localMember == null) {
            synchronized ($memberLock) {
                localMember = memberService.get(member);
                if (localMember == null) {
                    localMember = new LocalMember();
                    localMember.setGuildId(member.getGuild().getIdLong());
                    localMember.setUser(getOrCreate(member.getUser()));
                    localMember.setEffectiveName(member.getEffectiveName());
                    updateIfRequired(member, localMember);
                    memberRepository.flush();
                    return localMember;
                }
            }
        }
        return updateIfRequired(member, localMember);
    }

    private GuildConfig updateIfRequred(Guild guild, GuildConfig config) {
        try {
            boolean shouldSave = false;
            if (!Objects.equals(config.getName(), guild.getName())) {
                config.setName(guild.getName());
                shouldSave = true;
            }
            if (!Objects.equals(config.getIconUrl(), guild.getIconUrl())) {
                config.setIconUrl(guild.getIconUrl());
                shouldSave = true;
            }
            if (shouldSave) {
                configService.save(config);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return config;
    }

    private LocalUser updateIfRequired(User user, LocalUser localUser) {
        try {
            boolean shouldSave = false;
            if (localUser.getId() == null) {
                shouldSave = true;
            }

            if (user != null) {
                if (!Objects.equals(user.getName(), localUser.getName())) {
                    localUser.setName(user.getName());
                    shouldSave = true;
                }

                if (!Objects.equals(user.getDiscriminator(), localUser.getDiscriminator())) {
                    localUser.setDiscriminator(user.getDiscriminator());
                    shouldSave = true;
                }

                if (!Objects.equals(user.getAvatarUrl(), localUser.getAvatarUrl())) {
                    localUser.setAvatarUrl(user.getAvatarUrl());
                    shouldSave = true;
                }
            }
            if (shouldSave) {
                localUser = userService.save(localUser);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return localUser;
    }

    private LocalMember updateIfRequired(Member member, LocalMember localMember) {
        try {
            boolean shouldSave = false;
            if (localMember.getId() == null) {
                shouldSave = true;
            }
            if (member != null) {
                updateIfRequired(member.getUser(), localMember.getUser());
            }
            if (shouldSave) {
                memberService.save(localMember);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return localMember;
    }
}
