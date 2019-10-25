/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.message.resolver;

import lombok.NonNull;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.context.ApplicationContext;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.Ranking;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.utils.RankingUtils;
import ru.juniperbot.common.worker.message.resolver.node.AccessorsNodePlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.node.SingletonNodePlaceholderResolver;
import ru.juniperbot.common.worker.message.service.MessageService;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class MemberPlaceholderResolver extends AccessorsNodePlaceholderResolver<Member> {

    private final Member member;

    private volatile RankingInfo rankingInfo;

    public MemberPlaceholderResolver(@NonNull Member member,
                                     @NonNull Locale locale,
                                     @NonNull ApplicationContext applicationContext) {

        super(locale, applicationContext);
        this.member = member;
    }

    @Override
    protected Map<String, Supplier<?>> createAccessors() {
        return Map.ofEntries(
                Map.entry("id", () -> member.getUser().getId()),
                Map.entry("mention", member::getAsMention),
                Map.entry("nickname", member::getEffectiveName),
                Map.entry("name", () -> member.getUser().getName()),
                Map.entry("discriminator", () -> member.getUser().getDiscriminator()),
                Map.entry("joinedAt", () -> DateTimePlaceholderResolver.of(member.getTimeJoined(), locale, member.getGuild(), context)),
                Map.entry("createdAt", () -> DateTimePlaceholderResolver.of(member.getUser().getTimeCreated(), locale, member.getGuild(), context)),
                Map.entry("status", () -> context.getBean(MessageService.class).getEnumTitle(member.getOnlineStatus())),
                Map.entry("avatarUrl", () -> member.getUser().getEffectiveAvatarUrl()),
                Map.entry("level", () -> getRankingInfo().getLevel()),
                Map.entry("cookies", () -> getRankingInfo().getCookies()),
                Map.entry("voiceTime", () -> CommonUtils.formatDuration(getRankingInfo().getVoiceActivity())));
    }

    private RankingInfo getRankingInfo() {
        if (rankingInfo != null) {
            return rankingInfo;
        }
        synchronized (this) {
            if (rankingInfo == null) {
                Ranking ranking = context.getBean(RankingConfigService.class).getRanking(member);
                rankingInfo = ranking != null ? RankingUtils.calculateInfo(ranking) : new RankingInfo();
            }
        }
        return rankingInfo;
    }

    @Override
    protected Member getObject() {
        return member;
    }

    @Override
    public Object getValue() {
        return member.getAsMention();
    }

    public static MemberPlaceholderResolver of(@NonNull Member member,
                                               @NonNull Locale locale,
                                               @NonNull ApplicationContext context) {
        return new MemberPlaceholderResolver(member, locale, context);
    }

    public static SingletonNodePlaceholderResolver of(@NonNull Member member,
                                                      @NonNull Locale locale,
                                                      @NonNull ApplicationContext context,
                                                      @NonNull String name) {
        return new SingletonNodePlaceholderResolver(name, new MemberPlaceholderResolver(member, locale, context));
    }
}
