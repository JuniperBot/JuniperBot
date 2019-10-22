package ru.juniperbot.common.worker.command.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.LocalUser;

@Getter
@Setter
public class MemberReference {

    private String id;

    private Member member;

    private LocalMember localMember;

    private User user;

    private LocalUser localUser;
}
