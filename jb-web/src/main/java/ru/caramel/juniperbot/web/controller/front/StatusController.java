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
package ru.caramel.juniperbot.web.controller.front;

import com.codahale.metrics.*;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

import java.util.*;
import java.util.function.Function;

@Controller
@Navigation(PageElement.STATUS)
public class StatusController extends AbstractController {

    @Autowired
    private MetricRegistry metricRegistry;

    @RequestMapping("/status")
    public ModelAndView status() {
        ModelAndView mv = new ModelAndView("status");
        Map<String, Metric> metricMap = metricRegistry.getMetrics();
        mv.addObject("guildCount", getMetricValue(metricMap, DiscordService.GAUGE_GUILDS));
        mv.addObject("userCount", getMetricValue(metricMap, DiscordService.GAUGE_USERS));
        mv.addObject("textChannelCount", getMetricValue(metricMap, DiscordService.GAUGE_TEXT_CHANNELS));
        mv.addObject("voiceChannelCount", getMetricValue(metricMap, DiscordService.GAUGE_VOICE_CHANNELS));
        mv.addObject("activeConnections", getMetricValue(metricMap, PlayerService.ACTIVE_CONNECTIONS));
        mv.addObject("jvmUptime", getMetricValue(metricMap, "jvm.uptime", e -> {
            Date date = new Date();
            date.setTime(date.getTime() - (long) e);
            return new PrettyTime(LocaleContextHolder.getLocale()).format(date);
        }));
        mv.addObject("executedCommands", getMetricValue(metricMap, "commands.executions.persist"));
        setMeterValue(mv, metricMap, "commands.executions.rate", "commandsRate");
        return mv;
    }

    private static Object getMetricValue(Map<String,Metric> metricMap, String name) {
        return getMetricValue(metricMap, name, null);
    }

    private static Object getMetricValue(Map<String,Metric> metricMap, String name, Function<Object, String> valueExtractor) {
        Metric metric = metricMap.get(name);
        Object value = null;
        if (metric instanceof Gauge) {
            Gauge gauge = (Gauge) metric;
            value = gauge.getValue();

        }
        if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            value = counter.getCount();
        }
        if (value != null && valueExtractor != null) {
            value = valueExtractor.apply(value);
        }
        return value;
    }

    private static String setMeterValue(ModelAndView mv, Map<String,Metric> metricMap, String name, String attributeName) {
        Metric metric = metricMap.get(name);
        if (metric instanceof Meter) {
            Meter meter = (Meter) metric;
            mv.addObject(attributeName + "Mean", meter.getMeanRate());
            mv.addObject(attributeName + "1m", meter.getOneMinuteRate());
            mv.addObject(attributeName + "5m", meter.getFiveMinuteRate());
            mv.addObject(attributeName + "15m", meter.getFifteenMinuteRate());
        }
        return null;
    }
}
