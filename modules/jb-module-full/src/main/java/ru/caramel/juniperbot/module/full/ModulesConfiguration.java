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
package ru.caramel.juniperbot.module.full;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.caramel.juniperbot.module.audio.AudioConfiguration;
import ru.caramel.juniperbot.module.custom.CustomConfiguration;
import ru.caramel.juniperbot.module.groovy.GroovyConfiguration;
import ru.caramel.juniperbot.module.info.InfoConfiguration;
import ru.caramel.juniperbot.module.junipost.PostConfiguration;
import ru.caramel.juniperbot.module.mafia.MafiaConfiguration;
import ru.caramel.juniperbot.module.misc.MiscConfiguration;
import ru.caramel.juniperbot.module.moderation.ModerationConfiguration;
import ru.caramel.juniperbot.module.ranking.RankingConfiguration;
import ru.caramel.juniperbot.module.reminder.ReminderConfiguration;
import ru.caramel.juniperbot.module.steam.SteamConfiguration;
import ru.caramel.juniperbot.module.vk.VkConfiguration;
import ru.caramel.juniperbot.module.welcome.WelcomeConfiguration;
import ru.caramel.juniperbot.module.wikifur.WikiFurConfiguration;

@Import({
        AudioConfiguration.class,
        PostConfiguration.class,
        SteamConfiguration.class,
        GroovyConfiguration.class,
        WikiFurConfiguration.class,
        WelcomeConfiguration.class,
        VkConfiguration.class,
        MiscConfiguration.class,
        ModerationConfiguration.class,
        RankingConfiguration.class,
        InfoConfiguration.class,
        ReminderConfiguration.class,
        CustomConfiguration.class,
        MafiaConfiguration.class
})
@Configuration
public class ModulesConfiguration {
}
