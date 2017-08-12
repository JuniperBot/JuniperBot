package ru.caramel.juniperbot.persistence.entity.base;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class MemberEntity extends GuildEntity {

    @Setter
    @Getter
    @Basic
    protected String userId;

}
