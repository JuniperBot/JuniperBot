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
package ru.caramel.juniperbot.core.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class ExecutionConfiguration {

    @Value("${core.taskExecutor.corePoolSize:5}")
    private int taskExecutorCorePoolSize;

    @Value("${core.taskExecutor.maxPoolSize:5}")
    private int taskExecutorMaxPoolSize;

    @Value("${core.eventExecutor.corePoolSize:5}")
    private int eventExecutorCorePoolSize;

    @Value("${core.eventExecutor.maxPoolSize:5}")
    private int eventExecutorMaxPoolSize;

    @Primary
    @Bean("executor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutorCorePoolSize);
        executor.setMaxPoolSize(taskExecutorMaxPoolSize);
        executor.setThreadNamePrefix("taskExecutor");
        return executor;
    }

    @Bean("eventManagerExecutor")
    public TaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(eventExecutorCorePoolSize);
        executor.setMaxPoolSize(eventExecutorMaxPoolSize);
        executor.setThreadNamePrefix("eventExecutor");
        return executor;
    }
}
