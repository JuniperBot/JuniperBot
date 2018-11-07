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
package ru.caramel.juniperbot.core.support;

import ch.qos.logback.classic.LoggerContext;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.logback.SentryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.service.impl.DiscordServiceImpl;

import javax.annotation.PostConstruct;

@Component
public class SentryService {

    private static final Logger log = LoggerFactory.getLogger(DiscordServiceImpl.class);

    @Value("${build.version}")
    private String appVersion;

    @Value("${core.sentryDsn:}")
    private String sentryDsn;

    @PostConstruct
    public void initSentry() {
        if (sentryDsn == null || sentryDsn.isEmpty()) {
            log.info("No Sentry DSN found, turning it off.");
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            SentryAppender sentryAppender = (SentryAppender) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("SENTRY");
            if (sentryAppender != null) {
                sentryAppender.stop();
            }
            return;
        }
        SentryClient sentryClient = Sentry.init(sentryDsn);
        sentryClient.setRelease(appVersion);
        log.info("Sentry for release {} initialized.", appVersion);
    }
}
