package ru.caramel.juniperbot.core.modules.ranking.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Role;

@Getter
@Setter
public class RewardDetails extends Reward {
    private static final long serialVersionUID = -1441732862370850282L;

    private Role role;

    public RewardDetails(Role role, Reward reward) {
        this.role = role;
        this.roleId = reward.roleId;
        this.level = reward.level;
    }
}
