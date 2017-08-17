package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import ru.caramel.juniperbot.persistence.entity.base.BaseEntity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(indexes = { @Index(columnList = "guildId", name = "idx_guild_config_guildId", unique = true) })
public class GuildConfig extends BaseEntity {

    private static final long serialVersionUID = 1599157155969887890L;

    @Basic
    private long guildId;

    @Basic
    @NotEmpty
    @Size(max = 20)
    private String prefix;

    public GuildConfig(long guildId) {
        this.guildId = guildId;
    }
}
