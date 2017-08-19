package ru.caramel.juniperbot.persistence.entity.base;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class MemberEntity extends GuildEntity {

    @Setter
    @Getter
    @Column(name = "user_id")
    protected String userId;

}
