package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "web_hook")
@ToString
@Getter
@Setter
public class WebHook extends BaseEntity {

    private static final long serialVersionUID = 5589056134859236418L;

    @OneToOne(mappedBy = "webHook", fetch = FetchType.LAZY)
    private GuildConfig config;

    @Column(name = "hook_id")
    private Long hookId;

    @Column(name = "token")
    private String token;

    @Column
    private boolean enabled;
}
