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

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.caramel.juniperbot.core.support.*;

@EnableAspectJAutoProxy
@EnableTransactionManagement
@EntityScan("ru.caramel.juniperbot")
@EnableJpaRepositories("ru.caramel.juniperbot")
@ComponentScan("ru.caramel.juniperbot")
@Import({
        ExecutionConfiguration.class,
        SchedulerConfiguration.class,
        CacheConfiguration.class,
        MetricsConfiguration.class
})
@Configuration
public class CoreConfiguration {

    @Bean
    public ModuleMessageSourceImpl coreMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("core-jbmessages");
        return source;
    }

    @Bean
    public ModuleMessageSourceImpl moderationMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("moderation-jbmessages");
        return source;
    }

    @Bean
    public ModuleMessageSourceImpl auditMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("audit-jbmessages");
        return source;
    }

    @Bean
    public MessageSource messageSource() {
        return new JbMessageSource();
    }
}
