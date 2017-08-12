package ru.caramel.juniperbot.persistence.entity.base;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class GuildEntity extends BaseEntity {

    @Setter
    @Getter
    @Basic
    protected String guildId;

}
