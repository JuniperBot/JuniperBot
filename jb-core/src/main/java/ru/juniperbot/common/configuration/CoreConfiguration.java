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
package ru.juniperbot.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.juniperbot.common.support.JbCacheManager;
import ru.juniperbot.common.support.JbCacheManagerImpl;
import ru.juniperbot.common.support.jmx.ThreadPoolTaskExecutorMBean;

@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EntityScan({"ru.caramel.juniperbot", "ru.juniperbot"})
@EnableJpaRepositories({"ru.caramel.juniperbot", "ru.juniperbot"})
@ComponentScan({"ru.caramel.juniperbot", "ru.juniperbot"})
@Import({
        MBeanConfiguration.class
})
@Configuration
public class CoreConfiguration {

    public static final String COMMON_SCHEDULER_NAME = "jbCommonTaskScheduler";

    @Value("${core.taskExecutor.corePoolSize:5}")
    private int taskExecutorCorePoolSize;

    @Value("${core.taskExecutor.maxPoolSize:5}")
    private int taskExecutorMaxPoolSize;

    @Value("${core.scheduler.poolSize:10}")
    private int schedulerPoolSize;

    @Primary
    @Bean(COMMON_SCHEDULER_NAME)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setThreadNamePrefix("taskScheduler");
        return scheduler;
    }

    @Primary
    @Bean("executor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutorCorePoolSize);
        executor.setMaxPoolSize(taskExecutorMaxPoolSize);
        executor.setThreadNamePrefix("taskExecutor");
        return executor;
    }

    @Primary
    @Bean("cacheManager")
    public JbCacheManager cacheManager() {
        return new JbCacheManagerImpl();
    }

    @Bean
    public ThreadPoolTaskExecutorMBean taskExecutorMBean() {
        return new ThreadPoolTaskExecutorMBean("Spring TaskExecutor", (ThreadPoolTaskExecutor) taskExecutor());
    }
}
