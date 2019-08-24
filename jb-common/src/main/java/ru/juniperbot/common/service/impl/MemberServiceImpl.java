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
package ru.juniperbot.common.service.impl;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.repository.LocalMemberRepository;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.service.UserService;

import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

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
    public LocalMember save(LocalMember member) {
        return memberRepository.save(member);
    }

    @Override
    public boolean isApplicable(Member member) {
        return member != null && !member.getGuild().getSelfMember().equals(member)
                && userService.isApplicable(member.getUser());
    }
}
