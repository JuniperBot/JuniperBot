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
package ru.caramel.juniperbot.module.welcome;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.juniperbot.common.support.ModuleMessageSourceImpl;

@Configuration
public class WelcomeConfiguration {

    @Bean
    public ModuleMessageSourceImpl welcomeMessages() {
        ModuleMessageSourceImpl source = new ModuleMessageSourceImpl();
        source.setBasename("welcome-jbmessages");
        return source;
    }
}
