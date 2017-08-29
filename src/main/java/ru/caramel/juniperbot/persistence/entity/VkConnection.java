package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.model.VkConnectionStatus;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "vk_connection")
public class VkConnection extends BaseEntity {
    private static final long serialVersionUID = 2146901518074674594L;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "web_hook_id")
    private WebHook webHook;

    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_config_id")
    private GuildConfig config;

    @Column(name = "group_id")
    private Integer groupId;

    @Column
    private String token;

    @Column
    private String name;

    @Column(name = "confirm_code")
    private String confirmCode;

    @Column
    @Enumerated(EnumType.STRING)
    private VkConnectionStatus status;

}
