/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.jail.event.NucleusJailEvent;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import io.github.nucleuspowered.nucleus.modules.fly.FlyPermissions;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.server.ServerWorld;

public class FlyListener implements IReloadableService.Reloadable, ListenerBase {

    private final INucleusServiceCollection serviceCollection;
    private FlyConfig flyConfig;

    @Inject
    public FlyListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.flyConfig = serviceCollection.configProvider().getDefaultModuleConfig(FlyConfig.class);
    }

    // Do it first, so other plugins can have a say.
    @Listener(order = Order.FIRST)
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer pl) {
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        if (this.flyConfig.isPermissionOnLogin() && !this.serviceCollection.permissionService().hasPermission(pl, FlyPermissions.BASE_FLY)) {
            this.safeTeleport(pl);
            return;
        }

        this.serviceCollection.storageManager().getUser(pl.uniqueId()).thenAccept(x -> x.ifPresent(y -> {
            if (y.get(FlyKeys.FLY_TOGGLE).orElse(false)) {
                if (Sponge.server().onMainThread()) {
                    this.exec(pl);
                } else {
                    Sponge.server().scheduler().submit(
                            Task.builder().execute(() -> this.exec(pl)).plugin(this.serviceCollection.pluginContainer()).build());
                }
            } else {
                if (Sponge.server().onMainThread()) {
                    this.safeTeleport(pl);
                } else {
                    Sponge.server().scheduler().submit(
                            Task.builder().execute(() -> this.safeTeleport(pl)).plugin(this.serviceCollection.pluginContainer()).build());
                }
            }
        }));
    }

    private void exec(final Player pl) {
        pl.offer(Keys.CAN_FLY, true);

        // If in the air, flying!
        if (pl.location().add(0, -1, 0).blockType() == BlockTypes.AIR.get()) {
            pl.offer(Keys.IS_FLYING, true);
        }
    }

    @Listener
    public void onPlayerQuit(final ServerSideConnectionEvent.Leave event, @Getter("player") final ServerPlayer pl) {
        if (!this.flyConfig.isSaveOnQuit()) {
            return;
        }

        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        this.serviceCollection.storageManager().getOrCreateUser(pl.uniqueId())
                .thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, pl.get(Keys.CAN_FLY).orElse(false)));

    }

    // Only fire if there is no cancellation at the end.
    @Listener(order = Order.LAST)
    public void onPlayerTransferWorld(final ChangeEntityWorldEvent.Post event,
                                      @Getter("entity") final ServerPlayer pl,
                                      @Getter("originalWorld") final ServerWorld twfrom,
                                      @Getter("destinationWorld") final ServerWorld twto) {
        if (shouldIgnoreFromGameMode(pl)) {
            return;
        }

        // If we have a subject, and this happens...
        final boolean isFlying = pl.get(Keys.IS_FLYING).orElse(false);

        // If we're moving world...
        if (!twfrom.key().equals(twto.key())) {
            // Next tick, they can fly... if they have permission to do so.
            Sponge.server().scheduler().submit(Task.builder().execute(() -> {
                if (this.serviceCollection.permissionService().hasPermission(pl, FlyPermissions.BASE_FLY)) {
                    pl.offer(Keys.CAN_FLY, true);
                    if (isFlying) {
                        pl.offer(Keys.IS_FLYING, true);
                    }
                } else {
                    this.serviceCollection.storageManager().getOrCreateUser(pl.uniqueId()).thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, false));
                    pl.offer(Keys.CAN_FLY, false);
                    pl.offer(Keys.IS_FLYING, false);
                }
            }).plugin(this.serviceCollection.pluginContainer()).build());
        }
    }

    @Listener
    public void onJail(final NucleusJailEvent.Jailed event) {
        this.serviceCollection.storageManager().getUserService().setAndSave(event.getJailedUser(), FlyKeys.FLY_TOGGLE, false);
    }

    static boolean shouldIgnoreFromGameMode(final Player player) {
        final GameMode gm = player.get(Keys.GAME_MODE).orElse(GameModes.SURVIVAL.get());
        return (gm.equals(GameModes.CREATIVE) || gm.equals(GameModes.SPECTATOR));
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.flyConfig = this.serviceCollection.configProvider().getModuleConfig(FlyConfig.class);
    }

    private void safeTeleport(final ServerPlayer pl) {
        if (!pl.get(Keys.IS_FLYING).orElse(false) && this.flyConfig.isFindSafeOnLogin()) {
            // Try to bring the subject down.
            this.serviceCollection
                    .teleportService()
                    .teleportPlayerSmart(
                            pl,
                            pl.serverLocation(),
                            false,
                            true,
                            TeleportScanners.DESCENDING_SCAN.get()
                    );
        }
    }
}
