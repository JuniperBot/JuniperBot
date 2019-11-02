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

    private boolean authorSelected;

    public String getEffectiveName() {
        if (member != null) {
            return member.getEffectiveName();
        }
        if (user != null) {
            return user.getName();
        }
        if (localMember != null) {
            return localMember.getEffectiveName();
        }
        if (localUser != null) {
            return localUser.getName();
        }
        return null;
    }

    public String getEffectiveAvatarUrl() {
        if (user != null) {
            return user.getEffectiveAvatarUrl();
        }
        if (localUser != null) {
            return localUser.getAvatarUrl();
        }
        return null;
    }
}
