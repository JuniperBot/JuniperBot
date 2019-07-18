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
package ru.caramel.juniperbot.core.event.intercept;

import net.dv8tion.jda.core.events.Event;

public class FilterChainImpl<T extends Event> implements FilterChain<T> {

    private static final int INCREMENT = 10;

    private Filter[] filters = new Filter[0];

    /**
     * The int which is used to maintain the current position
     * in the filter chain.
     */
    private int pos = 0;


    /**
     * The int which gives the current number of filters in the chain.
     */
    private int n = 0;

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(T event) {
        if (pos < n) {
            Filter<T> filter = (Filter<T>) filters[pos++];
            if (filter != null) {
                filter.doFilter(event, this);
            }
        }
    }

    @Override
    public void reset() {
        pos = 0;
    }

    /**
     * Add a filter to the set of filters that will be executed in this chain.
     *
     * @param newFilter The Filter for the event to be executed
     */
    void addFilter(Filter<T> newFilter) {
        // Prevent the same filter being added multiple times
        for (Filter filter : filters) {
            if (filter == newFilter) {
                return;
            }
        }

        if (n == filters.length) {
            Filter[] newFilters =
                    new Filter[n + INCREMENT];
            System.arraycopy(filters, 0, newFilters, 0, n);
            filters = newFilters;
        }
        filters[n++] = newFilter;
    }
}
