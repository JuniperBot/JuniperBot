package ru.caramel.juniperbot.persistence.entity.base;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TextChannelEntity extends GuildEntity {

    @Setter
    @Getter
    @Column(name = "channel_id")
    protected String channelId;

}
