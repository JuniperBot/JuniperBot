package ru.caramel.juniperbot.persistence.entity.base;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageEmbed;

@MappedSuperclass
public class MemberMessageEntity extends MemberEntity {

    @Getter
    @Setter
    @Max(MessageEmbed.TEXT_MAX_LENGTH)
    @Basic
    protected String message;

    @Setter
    @Getter
    @Basic
    protected String channelId;

}
