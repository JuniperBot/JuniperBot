package ru.caramel.juniperbot.core.modules.ranking.utils;

import org.apache.commons.collections4.CollectionUtils;
import ru.caramel.juniperbot.core.modules.ranking.model.RankingConfigDto;
import ru.caramel.juniperbot.core.modules.ranking.model.RankingInfo;
import ru.caramel.juniperbot.core.modules.ranking.model.Reward;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;

import java.util.Objects;

public final class RankingUtils {

    public static final int MAX_LEVEL = 999;

    private RankingUtils(){
        // private constructor
    }

    public static long getLevelExp(int level) {
        return 5 * (level * level) + (50 * level) + 100;
    }

    public static long getLevelTotalExp(int level) {
        long exp = 0;
        for (int i = 0; i < level; i++) {
            exp += getLevelExp(i);
        }
        return exp;
    }

    public static int getLevelFromExp(long exp) {
        int level = 0;
        while (exp >= getLevelExp(level)) {
            exp -= getLevelExp(level);
            level++;
        }
        return level;
    }

    public static RankingInfo calculateInfo(LocalMember member) {
        RankingInfo info = new RankingInfo(member);
        info.setTotalExp(member.getExp());
        info.setLevel(getLevelFromExp(member.getExp()));
        long remaining = info.getTotalExp();
        for (int i = 0; i < info.getLevel(); i++) {
            remaining -= getLevelExp(i);
        }
        info.setRemainingExp(remaining);
        info.setLevelExp(getLevelExp(info.getLevel()));
        return info;
    }

    public static Integer getLevelForRole(RankingConfigDto dto, Long roleId) {
        String roleString = String.valueOf(roleId);
        return CollectionUtils.isNotEmpty(dto.getRewards()) ? dto.getRewards().stream()
                .filter(e -> Objects.equals(roleString, e.getRoleId()))
                .map(Reward::getLevel).findFirst().orElse(null) : null;
    }
}
