package ru.caramel.juniperbot.persistence.entity;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public class MessageOwnedEntity extends MemberOwnedEntity {

    @Setter
    @Getter
    @Basic
    private String channelId;

}
