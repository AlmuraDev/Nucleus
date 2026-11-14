/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezePlayerListener implements ListenerBase {

    private final IMessageProviderService messageProviderService;
    private final FreezePlayerService service;

    private final Map<UUID, Instant> lastFreezeNotification = new HashMap<>();

    @Inject
    public FreezePlayerListener(final INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.service = serviceCollection.getServiceUnchecked(FreezePlayerService.class);
    }

    @Listener
    public void onPlayerMovement(final MoveEntityEvent event, @Root final Player player) {
        this.checkForFrozen(event, player, "freeze.cancelmove");
    }

    @Listener
    public void onPlayerInteractBlock(final InteractEvent event, @Root final Player player) {
        this.checkForFrozen(event, player, "freeze.cancelinteract");
    }

    @Listener
    public void onPlayerInteractBlock(final InteractBlockEvent event, @Root final Player player) {
        this.checkForFrozen(event, player, "freeze.cancelinteractblock");
    }

    @Listener
    public void onPlayerDisconnect(final ServerSideConnectionEvent.Leave event, @Getter("player") final ServerPlayer player) {
        this.service.invalidate(player.uniqueId());
    }

    private boolean checkForFrozen(final Event event, final Player player, final String message) {
        if (this.service.getFromUUID(player.uniqueId())) {
            final Instant now = Instant.now();
            if (this.lastFreezeNotification.getOrDefault(player.uniqueId(), now).isBefore(now)) {
                this.messageProviderService.sendMessageTo(player, message);
                this.lastFreezeNotification.put(player.uniqueId(), now.plus(2, ChronoUnit.SECONDS));
            }

            if (event instanceof Cancellable) {
                ((Cancellable) event).setCancelled(true);
            }
            return true;
        }

        return false;
    }
}
