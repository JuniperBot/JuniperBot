package ru.caramel.juniperbot.persistence.entity;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class MemberOwnedEntity extends BaseEntity {

    @Setter
    @Getter
    @Basic
    private String guildId;

    @Setter
    @Getter
    @Basic
    private String userId;

}
