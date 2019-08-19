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
package ru.juniperbot.worker.common.shared.service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.worker.common.shared.model.SupportConfiguration;

import java.util.Objects;
import java.util.Set;

@Service
public class SupportServiceImpl implements SupportService {

    @Autowired
    private SupportConfiguration configuration;

    @Autowired
    private DiscordService discordService;

    @Override
    public void grantDonators(Set<String> donatorIds) {
        if (CollectionUtils.isEmpty(donatorIds) || !discordService.isConnected(configuration.getGuildId())) {
            return;
        }
        Role donatorRole = getDonatorRole();
        if (donatorRole == null) {
            return;
        }
        Guild guild = donatorRole.getGuild();
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            return;
        }
        donatorIds.stream()
                .map(guild::getMemberById)
                .filter(Objects::nonNull)
                .filter(e -> !e.getRoles().contains(donatorRole))
                .forEach(e -> guild.addRoleToMember(e, donatorRole).queue());
    }

    @Override
    public Guild getSupportGuild() {
        if (configuration.getGuildId() == null) {
            return null;
        }
        return discordService.getGuildById(configuration.getGuildId());
    }

    @Override
    public Role getDonatorRole() {
        if (configuration.getDonatorRoleId() == null) {
            return null;
        }
        Guild guild = getSupportGuild();
        return guild != null ? guild.getRoleById(configuration.getDonatorRoleId()) : null;
    }
}
