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
package ru.juniperbot.worker.common.shared.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.repository.LocalMemberRepository;

import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    private final Object $lock = new Object[0];

    @Autowired
    private LocalMemberRepository memberRepository;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<LocalMember> findLike(long guildId, String query) {
        return memberRepository.findLike(guildId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Member member) {
        return get(member.getGuild(), member.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Guild guild, User user) {
        return get(guild.getIdLong(), user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(long guildId, String userId) {
        return memberRepository.findByGuildIdAndUserId(guildId, userId);
    }

    @Override
    @Transactional
    public LocalMember getOrCreate(Member member) {
        if (!isApplicable(member)) {
            return null;
        }
        LocalMember localMember = get(member);
        if (localMember == null) {
            synchronized ($lock) {
                localMember = get(member);
                if (localMember == null) {
                    localMember = new LocalMember();
                    localMember.setGuildId(member.getGuild().getIdLong());
                    localMember.setUser(userService.getOrCreate(member.getUser()));
                    localMember.setEffectiveName(member.getEffectiveName());
                    updateIfRequired(member, localMember);
                    memberRepository.flush();
                    return localMember;
                }
            }
        }
        return updateIfRequired(member, localMember);
    }

    @Override
    @Transactional
    public LocalMember save(LocalMember member) {
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public LocalMember updateIfRequired(Member member, LocalMember localMember) {
        try {
            boolean shouldSave = false;
            if (localMember.getId() == null) {
                shouldSave = true;
            }
            if (member != null) {
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

    @Override
    public boolean isApplicable(Member member) {
        return member != null && !member.getGuild().getSelfMember().equals(member)
                && userService.isApplicable(member.getUser());
    }
}
