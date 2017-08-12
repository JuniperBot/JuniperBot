package ru.caramel.juniperbot.persistence.repository.base;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import ru.caramel.juniperbot.persistence.entity.base.TextChannelEntity;

import java.util.List;

@NoRepositoryBean
public interface TextChannelRepository<T extends TextChannelEntity> extends GuildRepository<T> {

    List<T> findByGuildIdAndChannelId(String guildId, String channelId);

    @Query("SELECT count(e) > 0 FROM #{#entityName} e WHERE e.channelId = :channelId AND e.guildId = :guildId")
    boolean exists(@Param("guildId") String guildId, @Param("channelId") String channelId);

    Long deleteByGuildIdAndChannelId(String guildId, String channelId);

    Long deleteByGuildId(String guildId);

}
