package ru.juniperbot.common.utils;

import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.Ranking;

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

    public static long getRemainingExp(long totalExp) {
        long remaining = totalExp;
        for (int i = 0; i < getLevelFromExp(totalExp); i++) {
            remaining -= getLevelExp(i);
        }
        return remaining;
    }

    public static RankingInfo calculateInfo(Ranking ranking) {
        RankingInfo info = new RankingInfo(ranking.getMember());
        info.setTotalExp(ranking.getExp());
        info.setLevel(getLevelFromExp(ranking.getExp()));
        info.setRemainingExp(getRemainingExp(ranking.getExp()));
        info.setLevelExp(getLevelExp(info.getLevel()));
        info.setCookies(ranking.getCookies());
        info.setVoiceActivity(ranking.getVoiceActivity());
        return info;
    }
}
