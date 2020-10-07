/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.afk.NucleusAFKService;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.modules.afk.AFKPermissions;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.events.AFKEvents;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.AdventureUtils;
import io.github.nucleuspowered.nucleus.util.PermissionMessageChannel;
import io.github.nucleuspowered.nucleus.util.Tuples;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Identifiable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

@APIService(NucleusAFKService.class)
public class AFKHandler implements NucleusAFKService, IReloadableService.Reloadable, ServiceBase {

    private final Map<UUID, AFKData> data = Maps.newConcurrentMap();
    private final INucleusServiceCollection serviceCollection;
    private AFKConfig config = new AFKConfig();

    @GuardedBy("lock")
    private final Set<UUID> activity = Sets.newHashSet();

    @GuardedBy("lock2")
    private final Multimap<UUID, UUID> disabledTracking = HashMultimap.create();
    private final Object lock = new Object();
    private final Object lock2 = new Object();

    private final String afkOption = "nucleus.afk.toggletime";
    private final String afkKickOption = "nucleus.afk.kicktime";

    @Inject
    public AFKHandler(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    public void stageUserActivityUpdate(final ServerPlayer player) {
        this.stageUserActivityUpdate(player.getUniqueId());
    }

    private void stageUserActivityUpdate(final UUID uuid) {
        synchronized (this.lock) {
            synchronized (this.lock2) {
                if (this.disabledTracking.containsKey(uuid)) {
                    return;
                }
            }

            this.activity.add(uuid);
        }
    }

    public void onTick() {
        synchronized (this.lock) {
            this.activity.forEach(u -> this.data.compute(u, ((uuid, afkData) -> afkData == null ? new AFKData(uuid) :
                    this.updateActivity(uuid, afkData))));
            this.activity.clear();
        }

        final List<UUID> uuidList = Sponge.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toList());

        // Remove all offline players.
        final Set<Map.Entry<UUID, AFKData>> entries = this.data.entrySet();
        entries.removeIf(refactor -> !uuidList.contains(refactor.getKey()));
        entries.stream().filter(x -> !x.getValue().cacheValid).forEach(x -> x.getValue().updateFromPermissions());

        final long now = System.currentTimeMillis();

        // Check AFK status.
        entries.stream().filter(x -> x.getValue().isKnownAfk && !x.getValue().willKick && x.getValue().timeToKick > 0).forEach(e -> {
            if (now - e.getValue().lastActivityTime > e.getValue().timeToKick) {
                // Kick them
                e.getValue().willKick = true;
                final NucleusTextTemplateImpl message = this.config.getMessages().getKickMessage();
                final Component t;
                if (message == null || message.isEmpty()) {
                    t = this.serviceCollection.messageProvider().getMessageForDefault("afk.kickreason");
                } else {
                    t = message.asComponent();
                }

                final NucleusTextTemplateImpl messageToServer = this.config.getMessages().getOnKick();

                Sponge.getServer().getPlayer(e.getKey()).ifPresent(player -> {
                    final Audience mc;
                    if (this.config.isBroadcastOnKick()) {
                        mc = Sponge.getServer();
                    } else {
                        mc = new PermissionMessageChannel(this.serviceCollection.permissionService(), AFKPermissions.AFK_NOTIFY);
                    }

                    // TODO: CSM for thread
                    final AFKEvents.Kick events = new AFKEvents.Kick(player, messageToServer.getForObject(player), mc,
                            Sponge.getServer().getCauseStackManager().getCurrentCause());
                    if (Sponge.getEventManager().post(events)) {
                        // Cancelled.
                        return;
                    }

                    final Component toSend = t instanceof NucleusTextTemplateImpl ? ((NucleusTextTemplateImpl) t).getForObject(player) : t.asComponent();
                    Sponge.getServer().getScheduler().createExecutor(this.serviceCollection.pluginContainer()).execute(() -> player.kick(toSend));
                    final Component eventMessage = events.getMessage();
                    if (!AdventureUtils.isEmpty(eventMessage)) {
                        events.getAudience().ifPresent(x -> x.sendMessage(eventMessage, MessageType.SYSTEM));
                    }
                });
            }
        });

        // Check AFK status.
        entries.stream().filter(x -> !x.getValue().isKnownAfk && x.getValue().timeToAfk > 0).forEach(e -> {
            if (now - e.getValue().lastActivityTime  > e.getValue().timeToAfk) {
                Sponge.getServer().getPlayer(e.getKey()).ifPresent(this::setAfkInternal);
            }
        });
    }

    public void invalidateAfkCache() {
        this.data.forEach((k, v) -> v.cacheValid = false);
    }

    public boolean isAFK(final UUID uuid) {
        return this.data.containsKey(uuid) && this.data.get(uuid).isKnownAfk;
    }

    private void setAfkInternal(final ServerPlayer player) {
        this.setAfkInternal(player.getUniqueId(), false);
    }

    public boolean setAfkInternal(final UUID uuid, final boolean force) {
        // final UUID uuid = player.getUniqueId();
        final AFKData a = this.data.compute(uuid, ((u, afkData) -> afkData == null ? new AFKData(u) : afkData));
        if (force) {
            a.isKnownAfk = false;
        } else if (a.isKnownAfk) {
            return false;
        }

        if (a.canGoAfk()) {
            // Don't accident undo setting AFK, remove any activity from the list.
            synchronized (this.lock) {
                this.activity.remove(uuid);
            }

            final Tuples.NullableTuple<Component, Audience> ttmc = this.getAFKMessage(Sponge.getServer().getPlayer(uuid).get(), true);
            final AFKEvents.To event = new AFKEvents.To(Sponge.getServer().getPlayer(uuid).get(), ttmc.getFirstUnwrapped(), ttmc.getSecondUnwrapped(), Sponge.getServer().getCauseStackManager()
                    .getCurrentCause());
            Sponge.getEventManager().post(event);
            this.actionEvent(event, "command.afk.to.nobc", "command.afk.to.console");

            a.isKnownAfk = true;
            return true;
        }

        return false;
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.configProvider().getModuleConfig(AFKConfig.class);
    }

    private AFKData updateActivity(final UUID uuid, final AFKData data) {
        return this.updateActivity(uuid, data, Sponge.getServer().getCauseStackManager().getCurrentCause());
    }

    private AFKData updateActivity(final UUID uuid, final AFKData data, final Cause cause) {
        data.lastActivityTime = System.currentTimeMillis();
        if (data.isKnownAfk) {
            data.isKnownAfk = false;
            data.willKick = false;
            Sponge.getServer().getPlayer(uuid).ifPresent(x -> {
                final Tuples.NullableTuple<Component, Audience> ttmc = this.getAFKMessage(x, false);
                final AFKEvents.From event = new AFKEvents.From(x, ttmc.getFirstUnwrapped(), ttmc.getSecondUnwrapped(), cause);
                Sponge.getEventManager().post(event);
                this.actionEvent(event, "command.afk.from.nobc", "command.afk.from.console");
            });

        }

        return data;
    }

    private void actionEvent(final AFKEvents event, final String key, @Nullable final String consoleKey) {
        final Component message = event.getMessage();
        if (AdventureUtils.getContent(message).matches("^\\s*$")) {
            event.getAudience().ifPresent(x -> x.sendMessage(message, MessageType.SYSTEM));
        } else {
            event.getAudience().ifPresent(x -> this.serviceCollection.messageProvider().sendMessageTo(x, key));
            if (consoleKey != null) {
                this.serviceCollection.messageProvider()
                        .sendMessageTo(Sponge.getSystemSubject(), consoleKey, event.getPlayer().getName());
            }
        }
    }

    private Tuples.NullableTuple<Component, Audience> getAFKMessage(final Player player, final boolean isAfk) {
        if (this.config.isBroadcastAfkOnVanish() || !player.get(Keys.VANISH).orElse(false)) {
            final NucleusTextTemplateImpl template = isAfk ? this.config.getMessages().getAfkMessage() : this.config.getMessages().getReturnAfkMessage();
            return Tuples.ofNullable(template.getForObject(player), Audience.audience(Sponge.getServer()));
        } else {
            return Tuples.ofNullable(null, Audience.empty());
        }
    }

    @Override public boolean canGoAFK(final UUID user) {
        return this.getData(user).canGoAfk();
    }

    @Override public boolean setAFK(final UUID player, final boolean isAfk) {
        final AFKData data = this.data.computeIfAbsent(player, AFKData::new);
        if (data.isKnownAfk == isAfk) {
            // Already AFK
            return false;
        }

        if (isAfk) {
            return this.setAfkInternal(player, false);
        } else {
            return !this.updateActivity(player, data).isKnownAfk;
        }
    }

    @Override public boolean canBeKicked(final UUID user) {
        return this.getData(user).canBeKicked();
    }

    @Override public Instant lastActivity(final UUID player) {
        return Instant.ofEpochMilli(this.data.computeIfAbsent(player, AFKData::new).lastActivityTime);
    }

    @Override public Optional<Duration> timeForInactivity(final UUID user) {
        final AFKData data = this.getData(user);
        if (data.canGoAfk()) {
            return Optional.of(Duration.ofMillis(data.timeToAfk));
        }

        return Optional.empty();
    }

    @Override public Optional<Duration> timeForKick(final UUID user) {
        final AFKData data = this.getData(user);
        if (data.canBeKicked()) {
            return Optional.of(Duration.ofMillis(data.timeToKick));
        }

        return Optional.empty();
    }

    @Override public void invalidateCachedPermissions() {
        this.invalidateAfkCache();
    }

    @Override public void updateActivityForUser(final UUID player) {
        this.stageUserActivityUpdate(player);
    }

    @Override public NoExceptionAutoClosable disableTrackingForPlayer(final UUID player, final Duration time) {
        // Disable tracking now with a new UUID.
        final Task n = Task.builder().execute(t -> {
            synchronized (this.lock2) {
                this.disabledTracking.remove(player, t.getUniqueId());
            }
        }).delay(time).plugin(this.serviceCollection.pluginContainer()).build();
        final ScheduledTask task = Sponge.getServer().getScheduler().submit(n);

        synchronized (this.lock2) {
            this.disabledTracking.put(player, task.getUniqueId());
        }

        return () -> {
            task.cancel();
            n.getConsumer().accept(task);
        };
    }

    private AFKData getData(final UUID uuid) {
        AFKData data = this.data.get(uuid);
        if (data == null) {
            // Prevent more checks
            data = new AFKData(uuid, false);
        }

        return data;
    }

    @Override
    public Collection<UUID> getAfk() {
        return this.getAfkStream(x -> true).map(Identifiable::getUniqueId).collect(Collectors.toList());
    }

    public Collection<ServerPlayer> getAfk(final Predicate<ServerPlayer> filter) {
        return this.getAfkStream(filter).collect(Collectors.toList());
    }

    private Stream<ServerPlayer> getAfkStream(final Predicate<ServerPlayer> filter) {
        return this.data.entrySet().stream()
                .filter(x -> x.getValue().isKnownAfk)
                .map(x -> Sponge.getServer().getPlayer(x.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .filter(filter);
    }

    final class AFKData {

        private final UUID uuid;

        private long lastActivityTime = System.currentTimeMillis();
        boolean isKnownAfk = false;
        private boolean willKick = false;

        private boolean cacheValid = false;
        private long timeToAfk = -1;
        private long timeToKick = -1;

        private AFKData(final UUID uuid) {
            this(uuid, true);
        }

        private AFKData(final UUID uuid, final boolean permCheck) {
            this.uuid = uuid;
            if (permCheck) {
                this.updateFromPermissions();
            }
        }

        private boolean canGoAfk() {
            this.cacheValid = false;
            this.updateFromPermissions();
            return this.timeToAfk > 0;
        }

        private boolean canBeKicked() {
            this.cacheValid = false;
            this.updateFromPermissions();
            return this.timeToKick > 0;
        }

        void updateFromPermissions() {
            synchronized (this) {
                if (!this.cacheValid) {
                    // Get the subject.
                    final IPermissionService service = AFKHandler.this.serviceCollection.permissionService();
                    Sponge.getServer().getPlayer(this.uuid).ifPresent(x -> {
                        if (service.hasPermission(x, AFKPermissions.AFK_EXEMPT_TOGGLE)) {
                            this.timeToAfk = -1;
                        } else {
                            this.timeToAfk = service.getPositiveLongOptionFromSubject(x,
                                    AFKHandler.this.afkOption).orElseGet(() -> AFKHandler.this.config.getAfkTime()) * 1000;
                        }

                        if (service.hasPermission(x, AFKPermissions.AFK_EXEMPT_KICK)) {
                            this.timeToKick = -1;
                        } else {
                            this.timeToKick = service.getPositiveLongOptionFromSubject(x,
                                    AFKHandler.this.afkKickOption).orElseGet(() -> AFKHandler.this.config.getAfkTimeToKick()) * 1000;
                        }

                        this.cacheValid = true;
                    });
                }
            }
        }
    }
}