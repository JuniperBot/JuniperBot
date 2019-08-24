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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.module.ranking.service.RankingService;

@Slf4j
@Component
@EnableRabbit
public class RankingQueueListener extends BaseQueueListener {

    @Autowired
    private RankingService rankingService;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_RANKING_UPDATE_REQUEST)
    public void updateRanking(RankingUpdateRequest request) {
        Guild guild = getGuildById(request.getGuildId());
        if (guild == null) {
            return;
        }

        Member member = guild.getMemberById(request.getUserId());
        if (member != null) {
            rankingService.updateRewards(member);
        }
    }
}
