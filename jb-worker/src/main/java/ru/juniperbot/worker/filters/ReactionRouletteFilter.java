/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.worker.filters;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.worker.common.event.intercept.Filter;
import ru.juniperbot.worker.common.event.intercept.FilterChain;
import ru.juniperbot.worker.common.event.intercept.MemberMessageFilter;
import ru.juniperbot.common.persistence.entity.ReactionRoulette;
import ru.juniperbot.common.service.ReactionRouletteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Order(Filter.POST_FILTER)
@Component
public class ReactionRouletteFilter extends MemberMessageFilter {

    @Autowired
    private ReactionRouletteService reactionRouletteService;

    private Map<String, AtomicInteger> channelThresholds = new HashMap<>();

    @Override
    @Transactional
    public void doInternal(GuildMessageReceivedEvent event, FilterChain<GuildMessageReceivedEvent> chain) {
        Guild guild = event.getGuild();
        if (guild.getSelfMember().equals(event.getMember())) {
            chain.doFilter(event);
            return;
        }
        ReactionRoulette roulette = reactionRouletteService.getByGuildId(guild.getIdLong());
        if (roulette == null
                || !roulette.isEnabled()
                || CollectionUtils.isEmpty(roulette.getSelectedEmotes())) {
            chain.doFilter(event);
            return;
        }

        if (CollectionUtils.isNotEmpty(roulette.getIgnoredChannels())
                && roulette.getIgnoredChannels().contains(event.getChannel().getIdLong())) {
            chain.doFilter(event);
            return;
        }

        final AtomicInteger threshold = channelThresholds.computeIfAbsent(event.getChannel().getId(),
                e -> new AtomicInteger());

        if (threshold.getAndIncrement() > roulette.getThreshold()
                && RandomUtils.nextLong(1, 1000) <= roulette.getPercent() * 10) {
            threshold.set(0);
            List<Emote> emotes = guild.getEmotes().stream()
                    .filter(e -> !e.isManaged() && roulette.getSelectedEmotes().contains(e.getId()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(emotes)) {
                Emote emote = emotes.get(RandomUtils.nextInt(0, emotes.size()));
                if (roulette.isReaction()
                        && guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION)
                        && emote.canInteract(event.getJDA().getSelfUser(), event.getChannel())) {
                    try {
                        event.getMessage().addReaction(emote).queue();
                    } catch (Exception e) {
                        // ignore
                    }
                } else if (guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
                    event.getChannel().sendMessage(emote.getAsMention()).queue();
                }
            }
        }
        chain.doFilter(event);
    }
}
