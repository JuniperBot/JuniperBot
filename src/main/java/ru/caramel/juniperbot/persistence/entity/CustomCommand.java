package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.model.enums.CommandType;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@Table(name = "custom_command")
public class CustomCommand extends BaseEntity {

    private static final long serialVersionUID = -8582315203089732918L;

    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_config_id")
    private GuildConfig config;

    @Column
    @Enumerated(EnumType.STRING)
    @NotNull
    private CommandType type;

    @Size(min = 1, max = 25)
    @NotNull
    private String key;

    @Column(columnDefinition = "text")
    @NotNull
    private String content;
}
