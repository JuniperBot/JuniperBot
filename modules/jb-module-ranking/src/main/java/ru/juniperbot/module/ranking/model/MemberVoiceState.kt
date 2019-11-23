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
package ru.juniperbot.module.ranking.model

import com.google.common.util.concurrent.AtomicDouble
import java.util.concurrent.atomic.AtomicLong

class MemberVoiceState {
    val activityTime = AtomicLong(0)
    val points = AtomicDouble(0.0)
    var lastAccumulated = System.currentTimeMillis()
    var frozen: Boolean = false
}