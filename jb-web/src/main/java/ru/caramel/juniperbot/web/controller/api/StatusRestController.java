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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.dv8tion.jda.core.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.model.TimeWindowChart;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.web.dto.ChartDto;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
public class StatusRestController extends BaseRestController {

    @Autowired
    private DiscordService discordService;

    private Supplier<List<ChartDto>> pingCache = Suppliers.memoizeWithExpiration(this::getPing, 5, TimeUnit.SECONDS);

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    @ResponseBody
    public List<ChartDto> ping() {
        return pingCache.get();
    }

    private synchronized List<ChartDto> getPing() {
        Map<JDA, TimeWindowChart> chartMap = discordService.getPingCharts();
        if (chartMap == null) {
            return Collections.emptyList();
        }
        List<JDA> shards = new ArrayList<>(discordService.getShardManager().getShards());
        shards.sort(Comparator.comparingInt(e -> e.getShardInfo().getShardId()));
        List<ChartDto> result = new ArrayList<>(shards.size());
        shards.forEach(jda -> {
            TimeWindowChart chart = chartMap.get(jda);
            if (chart != null) {
                ChartDto dto = new ChartDto(String.format(" Shard %s — %s ms", jda.getShardInfo().getShardId(), jda.getPing()));
                dto.setColor(jda.getShardInfo().getShardId());
                Map<Long, Long> measurements = chart.getMeasurements();
                if (measurements != null) {
                    Object[][] data = new Object[measurements.size()][2];
                    int i = 0;
                    for (Map.Entry<Long, Long> entry : measurements.entrySet()) {
                        Object[] part = data[i++];
                        part[0] = entry.getKey();
                        part[1] = entry.getValue();
                    }
                    dto.setData(data);
                }
                result.add(dto);
            }
        });
        return result;
    }
}
