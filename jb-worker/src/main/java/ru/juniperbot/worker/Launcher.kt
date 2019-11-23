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
package ru.juniperbot.worker

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import ru.juniperbot.common.support.ModuleMessageSource
import ru.juniperbot.common.support.ModuleMessageSourceImpl
import ru.juniperbot.common.worker.configuration.WorkerConfiguration
import ru.juniperbot.module.full.ModulesConfiguration


@SpringBootApplication
@Import(WorkerConfiguration::class, ModulesConfiguration::class)
class JuniperWorkerApplication {

    @Bean
    fun coreMessages(): ModuleMessageSource = ModuleMessageSourceImpl("common-jbmessages")

    @Bean
    fun jdaMessages(): ModuleMessageSource = ModuleMessageSourceImpl("jda-jbmessages")

    @Bean
    fun moderationMessages(): ModuleMessageSource = ModuleMessageSourceImpl("moderation-jbmessages")

    @Bean
    fun auditMessages(): ModuleMessageSource = ModuleMessageSourceImpl("audit-jbmessages")

    @Bean
    fun infoMessages(): ModuleMessageSource = ModuleMessageSourceImpl("info-jbmessages")

    @Bean
    fun reminderMessages(): ModuleMessageSource = ModuleMessageSourceImpl("reminder-jbmessages")

    @Bean
    fun steamMessages(): ModuleMessageSource = ModuleMessageSourceImpl("steam-jbmessages")
}

object Launcher {

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication(JuniperWorkerApplication::class.java).run(*args)
    }
}