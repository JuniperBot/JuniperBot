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
package ru.juniperbot.common.worker.shared.service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.worker.configuration.WorkerProperties;

import java.util.Objects;
import java.util.Set;

@Service
public class SupportServiceImpl implements SupportService {

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private DiscordService discordService;

    @Override
    public void grantDonators(Set<String> donatorIds) {
        if (CollectionUtils.isEmpty(donatorIds)) {
            return;
        }
        Role donatorRole = getDonatorRole();
        if (donatorRole == null) {
            return;
        }
        Guild guild = donatorRole.getGuild();
        if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
            donatorIds.stream()
                    .map(guild::getMemberById)
                    .filter(Objects::nonNull)
                    .filter(e -> !e.getRoles().contains(donatorRole))
                    .forEach(e -> guild.addRoleToMember(e, donatorRole).queue());
        }
    }

    @Override
    public Guild getSupportGuild() {
        Long guildId = workerProperties.getSupport().getGuildId();
        if (guildId == null || !discordService.isConnected(guildId)) {
            return null;
        }
        return discordService.getGuildById(guildId);
    }

    @Override
    public Role getDonatorRole() {
        Long donatorRoleId = workerProperties.getSupport().getDonatorRoleId();
        if (donatorRoleId == null) {
            return null;
        }
        Guild guild = getSupportGuild();
        return guild != null ? guild.getRoleById(donatorRoleId) : null;
    }
}
