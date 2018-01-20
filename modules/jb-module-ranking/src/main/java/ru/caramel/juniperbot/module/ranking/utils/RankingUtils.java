package ru.caramel.juniperbot.module.ranking.utils;

import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.persistence.entity.Ranking;

public final class RankingUtils {

    public static final int MAX_LEVEL = 999;

    private RankingUtils() {
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

    public static RankingInfo calculateInfo(Ranking ranking) {
        RankingInfo info = new RankingInfo(ranking.getMember());
        info.setTotalExp(ranking.getExp());
        info.setLevel(getLevelFromExp(ranking.getExp()));
        long remaining = info.getTotalExp();
        for (int i = 0; i < info.getLevel(); i++) {
            remaining -= getLevelExp(i);
        }
        info.setRemainingExp(remaining);
        info.setLevelExp(getLevelExp(info.getLevel()));
        info.setRank(ranking.getRank());
        return info;
    }
}
