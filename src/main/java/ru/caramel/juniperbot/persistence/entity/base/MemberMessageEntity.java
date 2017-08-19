package ru.caramel.juniperbot.persistence.entity.base;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageEmbed;

@MappedSuperclass
@Getter
@Setter
public abstract class MemberMessageEntity extends MemberEntity {

    @Size(max = MessageEmbed.TEXT_MAX_LENGTH)
    @Basic
    protected String message;

    @Column(name = "channel_id")
    protected String channelId;

}
