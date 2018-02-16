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
package ru.caramel.juniperbot.web.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;

public class TomcatUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatUtils.class);

    private TomcatUtils() {
        // private
    }

    public static boolean restartConnector(int port) {
        try {
            return restartConnectors(new ObjectName(String.format("*:type=Connector,port=%s,*", port)));
        } catch (Exception e) {
            LOGGER.warn("Connector[port={}] restart failed", port, e);
        }
        return false;
    }

    /**
     * Restarts connector for specified query. Keep in mind that is should be configured as <code>bindOnInit="false"</code>
     * in server.xml
     * @param query Connector Query
     * @return {@link true} if success
     */
    public static boolean restartConnectors(ObjectName query)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        for (final MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            if (server.queryNames(query, null).size() > 0) {
                ObjectName objectName = (ObjectName) server.queryNames(query, null).toArray()[0];
                server.invoke(objectName, "stop", null, null);
                // Polling sleep to reduce delay to safe minimum.
                // Use currentTimeMillis() over nanoTime() to avoid issues
                // with migrating threads across sleep() calls.
                long start = System.currentTimeMillis();
                // Maximum of 6 seconds, 3x time required on an idle system.
                long maxDuration = 6000L;
                long duration;
                do {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    duration = (System.currentTimeMillis() - start);
                } while (duration < maxDuration && server.queryNames(query, null).size() > 0);
                server.invoke(objectName, "start", null, null);
                return true;
            }
        }
        return false;
    }
}
