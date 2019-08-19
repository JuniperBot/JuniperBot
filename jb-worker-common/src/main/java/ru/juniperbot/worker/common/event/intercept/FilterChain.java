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
package ru.juniperbot.worker.common.event.intercept;

import net.dv8tion.jda.api.events.Event;

/**
 * A FilterChain is an object giving a view into the invocation chain of a filtered event for a resource.
 * Filters use the FilterChain to invoke the next filter in the chain, or if the
 * calling filter is the last filter in the chain, to invoke the resource at the
 * end of the chain.
 *
 * @see Filter
 **/
public interface FilterChain<T extends Event> {

    /**
     * Causes the next filter in the chain to be invoked, or if the calling
     * filter is the last filter in the chain, causes the resource at the end of
     * the chain to be invoked.
     *
     * @param event the event to pass along the chain.
     */
    void doFilter(T event);

    /**
     * Resets chain to be ready to handle next request
     */
    void reset();
}
