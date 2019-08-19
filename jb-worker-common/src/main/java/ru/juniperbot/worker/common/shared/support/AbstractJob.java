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
package ru.juniperbot.worker.common.shared.support;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractJob implements Job {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    protected void reschedule(JobExecutionContext context, TimeUnit unit, long duration) {
        log.info("Rescheduling job {}", context.getJobDetail());
        Trigger newTrigger = TriggerBuilder
                .newTrigger()
                .startAt(DateTime.now().plus(unit.toMillis(duration)).toDate())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .build();
        try {
            schedulerFactoryBean.getScheduler().rescheduleJob(context.getTrigger().getKey(), newTrigger);
        } catch (SchedulerException e) {
            log.warn("Could not reschedule job {}", context.getJobDetail(), e);
        }
    }
}
