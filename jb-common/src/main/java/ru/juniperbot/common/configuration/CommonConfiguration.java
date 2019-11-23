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
package ru.juniperbot.common.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.juniperbot.common.support.JbCacheManager;
import ru.juniperbot.common.support.JbCacheManagerImpl;
import ru.juniperbot.common.support.JbMessageSource;
import ru.juniperbot.common.support.jmx.ThreadPoolTaskExecutorMBean;

@EnableAsync
@EnableRetry
@EnableScheduling
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EntityScan("ru.juniperbot")
@EnableJpaRepositories("ru.juniperbot")
@ComponentScan("ru.juniperbot")
@Import({
        MBeanConfiguration.class,
        RabbitConfiguration.class
})
@Configuration
public class CommonConfiguration {

    public final static String SCHEDULER = "taskScheduler";

    @Autowired
    private CommonProperties commonProperties;

    @Bean(SCHEDULER)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(commonProperties.getExecution().getSchedulerPoolSize());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setThreadNamePrefix(SCHEDULER);
        return scheduler;
    }

    @Primary
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(commonProperties.getExecution().getCorePoolSize());
        executor.setMaxPoolSize(commonProperties.getExecution().getMaxPoolSize());
        executor.setThreadNamePrefix("taskExecutor");
        return executor;
    }

    @Primary
    @Bean("cacheManager")
    public JbCacheManager cacheManager() {
        return new JbCacheManagerImpl();
    }

    @Bean
    public MessageSource messageSource() {
        return new JbMessageSource();
    }

    @Bean
    public ThreadPoolTaskExecutorMBean taskExecutorMBean() {
        return new ThreadPoolTaskExecutorMBean("Spring TaskExecutor", (ThreadPoolTaskExecutor) taskExecutor());
    }
}
