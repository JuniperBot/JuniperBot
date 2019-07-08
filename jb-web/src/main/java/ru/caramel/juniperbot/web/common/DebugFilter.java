package ru.caramel.juniperbot.web.common;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class DebugFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return event.getThreadName().startsWith("JDA [20 / 24]") ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
