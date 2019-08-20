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
 * A filter is an object that performs filtering tasks Discord events
 * <br>
 * All implementation must have {@link org.springframework.core.annotation.Order} annotation to handle chain order
 *
 * @see Filter.PRE_FILTER
 * @see Filter.HANDLE_FILTER
 * @see Filter.POST_FILTER
 */
public interface Filter<T extends Event> {

    /**
     * Pre-stage for various permission checks, common filters, etc
     */
    int PRE_FILTER = 0;

    /**
     * Common handling stage
     */
    int HANDLE_FILTER = 1000;

    /**
     * Post-stage
     */
    int POST_FILTER = 2000;

    /**
     * The <code>doFilter</code> method of the Filter is called by the event
     * manager each time a event passed through the chain.
     * <p>
     *
     * @param event The event to process
     * @param chain Provides access to the next filter in the chain for this
     *              filter to pass the request and response to for further
     *              processing
     */
    void doFilter(T event, FilterChain<T> chain);
}
