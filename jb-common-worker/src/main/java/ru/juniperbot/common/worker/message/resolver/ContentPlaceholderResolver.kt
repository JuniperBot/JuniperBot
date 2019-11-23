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
package ru.juniperbot.common.worker.message.resolver

import org.springframework.util.PropertyPlaceholderHelper

class ContentPlaceholderResolver(private val content: String) : PropertyPlaceholderHelper.PlaceholderResolver {

    companion object {
        private val PATTERN = "^content:?(\\d*)$".toRegex()
        private val SPACE_PATTERN = "\\s+".toRegex();
    }

    val args: List<String> by lazy {
        content.split(SPACE_PATTERN)
    }

    override fun resolvePlaceholder(placeholderName: String): String? {
        val result = PATTERN.find(placeholderName) ?: return null
        val value = result.groupValues[1]
        if (value.isEmpty()) {
            return content
        }
        val index = value.toIntOrNull() ?: return ""
        return if (index > 0 && index <= args.size) args[index - 1] else ""
    }
}
