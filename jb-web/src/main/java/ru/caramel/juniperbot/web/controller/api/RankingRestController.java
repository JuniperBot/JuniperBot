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
package ru.caramel.juniperbot.web.controller.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
public class RankingRestController extends BaseRestController {

    @Autowired
    private RankingService rankingService;

    private LoadingCache<Long, List<RankingInfo>> rankingCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<Long, List<RankingInfo>>() {
                        public List<RankingInfo> load(Long serverId) {
                            return rankingService.getRankingInfos(serverId);
                        }
                    });

    @RequestMapping(value = "/ranking/list/{serverId}", method = RequestMethod.GET)
    @ResponseBody
    public List<RankingInfo> list(
            @PathVariable("serverId") long serverId) throws ExecutionException {
        return rankingService.isEnabled(serverId) ? rankingCache.get(serverId) : Collections.emptyList();
    }
}
