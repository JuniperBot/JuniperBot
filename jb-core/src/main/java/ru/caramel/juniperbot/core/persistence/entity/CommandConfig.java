package ru.caramel.juniperbot.core.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.persistence.entity.base.GuildEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "command_config")
public class CommandConfig extends GuildEntity {
    private static final long serialVersionUID = -816775204006628349L;

    @Column
    @NotNull
    private String key;

    @Column
    private boolean disabled;

    @Column(name = "delete_source")
    private boolean deleteSource;

    @Type(type = "jsonb")
    @Column(name = "allowed_roles", columnDefinition = "json")
    private List<Long> allowedRoles;

    @Type(type = "jsonb")
    @Column(name = "ignored_roles", columnDefinition = "json")
    private List<Long> ignoredRoles;

    @Type(type = "jsonb")
    @Column(name = "allowed_channels", columnDefinition = "json")
    private List<Long> allowedChannels;

    @Type(type = "jsonb")
    @Column(name = "ignored_channels", columnDefinition = "json")
    private List<Long> ignoredChannels;

}
