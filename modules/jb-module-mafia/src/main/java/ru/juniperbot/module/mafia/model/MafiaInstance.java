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
package ru.juniperbot.module.mafia.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import ru.juniperbot.module.mafia.service.base.MafiaStateHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class MafiaInstance {

    public static final String IGNORED_REASON = "$end$";

    private final String prefix;

    private final Locale locale;

    private final JDA jda;

    private final long channelId;

    private final long guildId;

    private Long goonChannelId;

    private List<MafiaPlayer> players = Collections.synchronizedList(new ArrayList<>(10));

    private volatile MafiaState state = MafiaState.CHOOSING;

    private ScheduledFuture<?> scheduledStep;

    private Date stepEndTime;

    private MafiaStateHandler handler;

    private Map<String, Object> attributes = new HashMap<>();

    private String endReason;

    private Set<String> listenedMessages = Collections.synchronizedSet(new HashSet<String>());

    private Map<MafiaActionType, MafiaPlayer> dailyActions = new ConcurrentHashMap<>();

    private Long activeTime;

    public MafiaInstance(@NonNull TextChannel channel, Locale locale, String prefix) {
        this.jda = channel.getJDA();
        this.channelId = channel.getIdLong();
        this.guildId = channel.getGuild().getIdLong();
        this.locale = locale;
        this.prefix = prefix;
        tick();
    }

    public TextChannel getChannel() {
        return jda.getTextChannelById(channelId);
    }

    public TextChannel getGoonChannel() {
        return goonChannelId != null ? jda.getTextChannelById(goonChannelId) : null;
    }

    public void setGoonChannel(TextChannel channel) {
        this.goonChannelId = channel != null ? channel.getIdLong() : null;
    }

    public Guild getGuild() {
        return jda.getGuildById(guildId);
    }

    public synchronized void tick() {
        activeTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttribute(String key, T value) {
        return (T) attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    public boolean done(User user) {
        if (scheduledStep != null) {
            if (scheduledStep.isDone() || scheduledStep.isCancelled()) {
                return false;
            } else {
                scheduledStep.cancel(false);
                scheduledStep = null;
            }
        }
        return handler == null || handler.onEnd(user, this);
    }

    public void stop() {
        if (scheduledStep != null && !scheduledStep.isDone() && !scheduledStep.isCancelled()) {
            scheduledStep.cancel(false);
            scheduledStep = null;
        }
    }

    public boolean isInState(MafiaState state) {
        return Objects.equals(this.state, state);
    }

    public MafiaPlayer getPlayerByUser(User user) {
        return players.stream().filter(e -> Objects.equals(user, e.getUser())).findFirst().orElse(null);
    }

    public boolean isPlayer(User user, MafiaRole role) {
        MafiaPlayer player = getPlayerByUser(user);
        return player != null && player.getRole() == role;
    }

    public boolean isPlayer(User user) {
        return players.stream().anyMatch(e -> e.isAlive() && Objects.equals(user, e.getUser()));
    }

    public boolean isPlayer(Member member) {
        return isPlayer(member.getUser());
    }

    public List<MafiaPlayer> getPlayersByRole(MafiaRole role) {
        return players.stream().filter(e -> e.isAlive() && Objects.equals(e.getRole(), role)).collect(Collectors.toList());
    }

    public MafiaPlayer getPlayerByRole(MafiaRole role) {
        return players.stream().filter(e -> e.isAlive() && Objects.equals(e.getRole(), role)).findFirst().orElse(null);
    }

    public MafiaPlayer getCop() {
        return getPlayerByRole(MafiaRole.COP);
    }

    public MafiaPlayer getBroker() {
        return getPlayerByRole(MafiaRole.BROKER);
    }

    public MafiaPlayer getDoctor() {
        return getPlayerByRole(MafiaRole.DOCTOR);
    }

    public List<MafiaPlayer> getGoons() {
        return getPlayersByRole(MafiaRole.GOON);
    }

    public boolean hasAnyMafia() {
        return players.stream().anyMatch(e -> e.isAlive() && e.getRole().isMafia());
    }

    public boolean hasAnyTownie() {
        return players.stream().anyMatch(e -> e.isAlive() && !e.getRole().isMafia());
    }

    public List<MafiaPlayer> getAlive() {
        return players.stream().filter(MafiaPlayer::isAlive).collect(Collectors.toList());
    }

    public String getGoonsMentions() {
        return getGoons().stream()
                .map(MafiaPlayer::getMember)
                .filter(Objects::nonNull)
                .map(Member::getAsMention)
                .collect(Collectors.joining(" "));
    }

    public MafiaState updateState(MafiaState state) {
        MafiaState current = this.state;
        this.state = state;
        return current;
    }

    public void setIgnoredReason() {
        setEndReason(IGNORED_REASON);
    }
}
