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
package ru.juniperbot.common.support.jmx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jmx.support.ConnectorServerFactoryBean;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.configuration.CommonProperties;

import javax.management.JMException;
import java.io.IOException;

@Service
@ConditionalOnProperty(prefix = "juniperbot.common.jmx", name = "enabled", havingValue = "true")
public class JmxService extends ConnectorServerFactoryBean {

    private static final String SERVICE_URL = "service:jmx:jmxmp://localhost:%s";

    @Autowired
    private CommonProperties commonProperties;

    @Override
    public void afterPropertiesSet() throws JMException, IOException {
        setServiceUrl(String.format(SERVICE_URL, commonProperties.getJmx().getPort()));
        super.afterPropertiesSet();
    }
}
