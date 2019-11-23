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
package ru.juniperbot.common.extensions

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import org.joda.time.DateTime
import org.springframework.scheduling.TaskScheduler


fun Guild.modifyMemberRolesDelayed(scheduler: TaskScheduler,
                                   member: Member,
                                   rolesToAdd: List<Role>,
                                   rolesToRemove: List<Role>,
                                   delay: Long?) {
    if (delay == null || delay <= 0L) {
        this.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue()
        return
    }
    val jda = member.jda
    val memberId = member.id
    val guildId = member.guild.id
    val roleToAddIds = rolesToAdd.map { it.id }
    val roleToRemoveIds = rolesToRemove.map { it.id }

    scheduler.schedule(action@{
        val sGuild = jda.getGuildById(guildId) ?: return@action
        val sMember = sGuild.getMemberById(memberId) ?: return@action
        val sRolesToAdd = roleToAddIds.mapNotNull { sGuild.getRoleById(it) }
        val sRolesToRemove = roleToRemoveIds.mapNotNull { sGuild.getRoleById(it) }
        if (sRolesToAdd.isNotEmpty() || sRolesToRemove.isNotEmpty()) {
            sGuild.modifyMemberRoles(sMember, sRolesToAdd, sRolesToRemove).queue()
        }
    }, DateTime.now().plus(delay).toDate())
}