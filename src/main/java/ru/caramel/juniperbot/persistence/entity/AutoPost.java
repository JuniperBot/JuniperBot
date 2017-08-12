package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.caramel.juniperbot.persistence.entity.base.TextChannelEntity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"channelId", "guildId"})})
@ToString
public class AutoPost extends TextChannelEntity {

    @Getter
    @Setter
    @Basic
    private String latestId;

    public AutoPost() {
        // default
    }

    public AutoPost(String guildId, String channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }
}
