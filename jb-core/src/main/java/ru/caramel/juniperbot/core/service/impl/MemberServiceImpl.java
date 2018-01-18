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
package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.persistence.repository.LocalMemberRepository;
import ru.caramel.juniperbot.core.service.MemberService;
import ru.caramel.juniperbot.core.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private LocalMemberRepository memberRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public LocalMember getOrCreate(Member member) {
        if (!isApplicable(member)) {
            return null;
        }
        LocalMember localMember = memberRepository.findByGuildIdAndUserId(member.getGuild().getId(),
                member.getUser().getId());
        if (localMember == null) {
            localMember = new LocalMember();
            localMember.setGuildId(member.getGuild().getId());
            localMember.setUser(userService.getOrCreate(member.getUser()));
        }
        return updateIfRequired(member, localMember);
    }

    @Transactional
    public LocalMember save(LocalMember member) {
        return memberRepository.save(member);
    }

    @Override
    public LocalMember updateIfRequired(Member member, LocalMember localMember) {
        try {
            boolean shouldSave = false;
            if (localMember.getId() == null) {
                shouldSave = true;
            }

            if (member != null) {
                if (!Objects.equals(member.getEffectiveName(), localMember.getEffectiveName())) {
                    localMember.setEffectiveName(member.getEffectiveName());
                    shouldSave = true;
                }
                userService.updateIfRequired(member.getUser(), localMember.getUser());
            }

            if (shouldSave) {
                memberRepository.save(localMember);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // it's ok to ignore optlock here, anyway it will be updated later
        }
        return localMember;
    }

    @Transactional
    public List<LocalMember> syncMembers(Guild guild) {
        List<LocalMember> members = memberRepository.findByGuildId(guild.getId());
        Map<String, LocalMember> membersMap = members.stream().collect(Collectors.toMap(u -> u.getUser().getUserId(), e -> e));
        for (Member member : guild.getMembers()) {
            if (isApplicable(member)) {
                LocalMember localMember = membersMap.get(member.getUser().getId());
                if (localMember == null) {
                    localMember = new LocalMember();
                    localMember.setGuildId(member.getGuild().getId());
                    localMember.setUser(userService.getOrCreate(member.getUser()));
                    members.add(localMember);
                }
            }
        }
        members.forEach(e -> {
            Member member = guild.getMemberById(e.getUser().getUserId());
            if (member != null) {
                if (isApplicable(member)) {
                    updateIfRequired(member, e);
                }
            }
        });
        return members;
    }

    @Override
    public boolean isApplicable(Member member) {
        return member != null && !member.getGuild().getSelfMember().equals(member)
                && userService.isApplicable(member.getUser());
    }
}
