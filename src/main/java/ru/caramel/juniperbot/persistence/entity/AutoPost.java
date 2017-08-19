package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.caramel.juniperbot.persistence.entity.base.TextChannelEntity;

import javax.persistence.*;

@Entity
@Table(name = "auto_post")
@ToString
@Getter
@Setter
public class AutoPost extends TextChannelEntity {

    private static final long serialVersionUID = 5900616915326953578L;

    @Column(name = "latest_id")
    private String latestId;

    public AutoPost() {
        // default
    }

    public AutoPost(String guildId, String channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }
}
