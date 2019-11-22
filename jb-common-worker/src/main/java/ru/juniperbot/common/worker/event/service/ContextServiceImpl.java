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
package ru.juniperbot.common.worker.event.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.common.utils.LocaleUtils;

import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
public class ContextServiceImpl implements ContextService {

    private static class ContextHolder {
        private Locale locale;
        private Color color;
        private Long guildId;
        private String userId;
    }

    private static final String MDC_GUILD = "guildId";

    private static final String MDC_USER = "userId";

    private final ThreadLocal<Locale> localeHolder = new NamedThreadLocal<>("ContextServiceImpl.Locale");

    private final ThreadLocal<Color> colorHolder = new NamedThreadLocal<>("ContextServiceImpl.Color");

    private final ThreadLocal<Long> guildHolder = new NamedThreadLocal<>("ContextServiceImpl.GuildIds");

    @Getter
    private Color accentColor;

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SourceResolverService resolverService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Locale getLocale() {
        Locale locale = localeHolder.get();
        if (locale == null) {
            Long guildId = guildHolder.get();
            if (guildId != null) {
                setLocale(configService.getLocale(guildId));
                locale = localeHolder.get();
            }
        }
        return locale != null ? locale : LocaleUtils.getDefaultLocale();
    }

    @Override
    public Color getColor() {
        Color color = colorHolder.get();
        if (color == null) {
            Long guildId = guildHolder.get();
            if (guildId != null) {
                setColor(Color.decode(configService.getColor(guildId)));
                color = colorHolder.get();
            }
        }
        return color != null ? color : getDefaultColor();
    }

    @Override
    public Color getDefaultColor() {
        if (accentColor == null) {
            String defaultAccentColor = commonProperties.getDiscord().getDefaultAccentColor();
            accentColor = StringUtils.isNotEmpty(defaultAccentColor) ? Color.decode(defaultAccentColor) : null;
        }
        return accentColor;
    }

    @Override
    public Locale getLocale(String localeName) {
        return LocaleUtils.getOrDefault(localeName);
    }

    @Override
    public Locale getLocale(Guild guild) {
        return getLocale(guild.getIdLong());
    }

    @Override
    public Locale getLocale(long guildId) {
        return LocaleUtils.getOrDefault(configService.getLocale(guildId));
    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == null) {
            localeHolder.remove();
        } else {
            localeHolder.set(locale);
        }
    }

    @Override
    public void setColor(Color color) {
        if (color == null) {
            colorHolder.remove();
        } else {
            colorHolder.set(color);
        }
    }

    public void setGuildId(Long guildId) {
        if (guildId == null) {
            guildHolder.remove();
        } else {
            guildHolder.set(guildId);
        }
    }

    @Override
    public void initContext(GenericEvent event) {
        Guild guild = null;
        User user = null;

        if (event instanceof GenericGuildMemberEvent) {
            GenericGuildMemberEvent memberEvent = (GenericGuildMemberEvent) event;
            guild = memberEvent.getMember().getGuild();
            user = memberEvent.getMember().getUser();
        }

        if (guild == null && event instanceof GenericGuildEvent) {
            guild = ((GenericGuildEvent) event).getGuild();
        }

        if (guild == null || user == null) {
            Member member = resolverService.getMember(event);
            if (member != null) {
                guild = member.getGuild();
                user = member.getUser();
            }
        }
        if (guild == null) {
            guild = resolverService.getGuild(event);
        }
        if (user == null) {
            user = resolverService.getUser(event);
        }
        if (guild != null) {
            initContext(guild);
        }
        if (user != null) {
            initContext(user);
        }
    }

    @Override
    public void initContext(Guild guild) {
        if (guild != null) {
            initContext(guild.getIdLong());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T withContext(Long guildId, Supplier<T> action) {
        Object[] holder = new Object[1];
        withContext(guildId, () -> {
            holder[0] = action.get();
        });
        return (T) holder[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T withContext(Guild guild, Supplier<T> action) {
        Object[] holder = new Object[1];
        withContext(guild, () -> {
            holder[0] = action.get();
        });
        return (T) holder[0];
    }

    @Override
    public void withContext(Long guildId, Runnable action) {
        withContext(guildId, this::initContext, action);
    }

    @Override
    public void withContext(Guild guild, Runnable action) {
        withContext(guild, this::initContext, action);
    }

    private <T> void withContext(T value, Consumer<T> init, Runnable action) {
        if (value == null) {
            action.run();
            return;
        }
        ContextHolder currentContext = getContext();
        resetContext();
        init.accept(value);
        try {
            action.run();
        } finally {
            setContext(currentContext);
        }
    }

    @Override
    @Async
    public void withContextAsync(Guild guild, Runnable action) {
        inTransaction(() -> withContext(guild, action));
    }

    @Override
    public void inTransaction(Runnable action) {
        try {
            transactionTemplate.execute(status -> {
                try {
                    action.run();
                } catch (ObjectOptimisticLockingFailureException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Async task results in error", e);
                }
                return null;
            });
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failed for object {} [id={}]", e.getPersistentClassName(), e.getIdentifier());
        }
    }

    @Override
    public <T> void queue(Guild guild, RestAction<T> action, Consumer<T> success) {
        action.queue(e -> withContext(guild, () -> success.accept(e)));
    }

    @Override
    public <T> void queue(Long guildId, RestAction<T> action, Consumer<T> success) {
        action.queue(e -> withContext(guildId, () -> success.accept(e)));
    }

    @Override
    public void initContext(User user) {
        if (user != null) {
            MDC.put(MDC_USER, user.getId());
        }
    }

    @Override
    public void initContext(long guildId) {
        MDC.put(MDC_GUILD, String.valueOf(guildId));
        guildHolder.set(guildId);
    }

    @Override
    public void resetContext() {
        MDC.remove(MDC_GUILD);
        MDC.remove(MDC_USER);
        guildHolder.remove();
        localeHolder.remove();
        colorHolder.remove();
    }

    public ContextHolder getContext() {
        ContextHolder holder = new ContextHolder();
        holder.guildId = guildHolder.get();
        holder.locale = localeHolder.get();
        holder.color = colorHolder.get();
        holder.userId = MDC.get(MDC_USER);
        return holder;
    }

    public void setContext(ContextHolder holder) {
        setLocale(holder.locale);
        setGuildId(holder.guildId);
        MDC.put(MDC_GUILD, String.valueOf(holder.guildId));
        MDC.put(MDC_USER, holder.userId);
    }

    private void setLocale(String tag) {
        setLocale(LocaleUtils.get(tag));
    }
}
