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
package ru.juniperbot.common.worker.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.juniperbot.common.configuration.CommonConfiguration;
import ru.juniperbot.common.support.jmx.ThreadPoolTaskExecutorMBean;

@Import({
        CommonConfiguration.class,
        QuartzConfiguration.class,
        MetricsConfiguration.class,
})
@EnableDiscoveryClient
@Configuration
public class WorkerConfiguration {

    @Autowired
    private WorkerProperties workerProperties;

    @Bean("eventManagerExecutor")
    public TaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerProperties.getEvents().getCorePoolSize());
        executor.setMaxPoolSize(workerProperties.getEvents().getMaxPoolSize());
        executor.setThreadNamePrefix("eventExecutor");
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutorMBean eventTaskExecutorMBean() {
        return new ThreadPoolTaskExecutorMBean("Event TaskExecutor", (ThreadPoolTaskExecutor) eventExecutor());
    }
}
