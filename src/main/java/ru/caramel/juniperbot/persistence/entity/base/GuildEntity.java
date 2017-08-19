package ru.caramel.juniperbot.persistence.entity.base;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class GuildEntity extends BaseEntity {

    @Setter
    @Getter
    @Column(name = "guild_id")
    protected String guildId;

}
