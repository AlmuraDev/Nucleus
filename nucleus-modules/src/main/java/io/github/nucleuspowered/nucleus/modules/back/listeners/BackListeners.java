/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.listeners;

import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.EventContextKeys;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.module.jail.NucleusJailService;
import io.github.nucleuspowered.nucleus.modules.back.BackPermissions;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfig;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.type.Exclude;

import com.google.inject.Inject;
import org.spongepowered.api.world.server.ServerLocation;

public class BackListeners implements IReloadableService.Reloadable, ListenerBase {

    private final BackHandler handler;
    private final IPermissionService permissionService;
    private BackConfig backConfig = new BackConfig();
    @Nullable private final NucleusJailService jailService;

    @Inject
    public BackListeners(final INucleusServiceCollection serviceCollection) {
        // TODO: Pluggable stuff.
        this.jailService = NucleusAPI.getJailService().orElse(null);
        this.handler = serviceCollection.getServiceUnchecked(BackHandler.class);
        this.permissionService = serviceCollection.permissionService();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.backConfig = serviceCollection.configProvider().getModuleConfig(BackConfig.class);
    }

    @Listener(order = Order.LAST)
    @Exclude(ChangeEntityWorldEvent.Reposition.class)
    public void onTeleportPlayer(final MoveEntityEvent event, @Getter("entity") final ServerPlayer pl) {
        if (this.backConfig.isOnTeleport() && this.check(event) &&
                this.getLogBack(pl) && this.permissionService.hasPermission(pl,
                BackPermissions.BACK_ONTELEPORT)) {
            this.handler.setLastLocation(pl.uniqueId(), ServerLocation.of(pl.world(), event.originalPosition()), pl.rotation());
        }
    }

    @Listener(order = Order.LAST)
    public void onWorldTransfer(final ChangeEntityWorldEvent.Reposition event, @Getter("entity") final ServerPlayer pl) {
        if (this.backConfig.isOnPortal() && this.getLogBack(pl) && this.permissionService.hasPermission(pl, BackPermissions.BACK_ONPORTAL)) {
            this.handler.setLastLocation(pl.uniqueId(), ServerLocation.of(event.originalWorld(), event.destinationPosition()), pl.rotation());
        }
    }

    @Listener
    public void onDeathEvent(final DestructEntityEvent.Death event, @Getter("entity") final ServerPlayer pl) {
        if (this.backConfig.isOnDeath() && this.getLogBack(pl) && this.permissionService.hasPermission(pl, BackPermissions.BACK_ONDEATH)) {
            this.handler.setLastLocation(pl.uniqueId(), pl.serverLocation(), pl.rotation());
        }
    }

    private boolean check(final MoveEntityEvent event) {
        // MoveEntityEvent fires for every step a player walks, not just teleports: recording those made /back return
        // you to where you stood a moment ago. Walking is MovementTypes.NATURAL; teleports are COMMAND, PLUGIN, etc.
        if (event.context().get(EventContextKeys.MOVEMENT_TYPE).filter(type -> type == MovementTypes.NATURAL.get()).isPresent()) {
            return false;
        }
        return !event.originalPosition().equals(event.destinationPosition());
    }

    private boolean getLogBack(final ServerPlayer player) {
        return !(this.jailService != null && this.jailService.isPlayerJailed(player.uniqueId())) && this.handler.isLoggingLastLocation(player.uniqueId());
    }
}
