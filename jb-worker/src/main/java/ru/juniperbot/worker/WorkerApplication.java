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
package ru.juniperbot.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ru.caramel.juniperbot.module.full.ModulesConfiguration;
import ru.juniperbot.common.support.ModuleMessageSourceImpl;
import ru.juniperbot.common.worker.configuration.WorkerConfiguration;

@SpringBootApplication
@Import({WorkerConfiguration.class, ModulesConfiguration.class})
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Bean
    public ModuleMessageSourceImpl coreMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("common-jbmessages");
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
    public ModuleMessageSourceImpl infoMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("info-jbmessages");
        return source;
    }

    @Bean
    public ModuleMessageSourceImpl reminderMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("reminder-jbmessages");
        return source;
    }
}
