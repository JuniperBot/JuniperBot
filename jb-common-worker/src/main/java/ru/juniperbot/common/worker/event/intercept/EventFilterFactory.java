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
package ru.juniperbot.common.worker.event.intercept;

import net.dv8tion.jda.api.events.Event;

/**
 * Factory for the creation and caching of Filters and creation
 * of Filter Chains.
 */
public interface EventFilterFactory<T extends Event> {

    /**
     * Construct a FilterChain implementation that will wrap the execution of
     * the specified servlet instance.
     *
     * @param event The event we are processing
     * @return The configured FilterChain instance or null if none is to be
     * executed.
     */
    FilterChain<T> createChain(T event);

    /**
     * Returns a type of event to process in this chain
     *
     * @return Type of event to process in this chain
     */
    Class<T> getType();
}
