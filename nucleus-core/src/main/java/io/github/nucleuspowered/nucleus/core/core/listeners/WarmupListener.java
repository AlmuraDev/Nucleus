/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.listeners;

import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IWarmupService;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;

import com.google.inject.Inject;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class WarmupListener implements IReloadableService.Reloadable, ListenerBase {

    private final IWarmupService warmupService;
    private final IMessageProviderService messageProviderService;
    private WarmupConfig warmupConfig = new WarmupConfig();

    @Inject
    public WarmupListener(final INucleusServiceCollection serviceCollection) {
        this.warmupService = serviceCollection.warmupService();
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener(order = Order.LAST)
    public void onPlayerMovement(final MoveEntityEvent event, @Root final ServerPlayer player) {
        // Rotating is OK!
        if (this.warmupConfig.isOnMove() && !event.originalDestinationPosition().equals(event.destinationPosition())) {
            this.cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerCommand(final ExecuteCommandEvent.Pre event, @Root final ServerPlayer player) {
        if (this.warmupConfig.isOnCommand()) {
            this.cancelWarmup(player);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(final ServerSideConnectionEvent.Leave event) {
        this.cancelWarmup(event.player());
    }

    private void cancelWarmup(final ServerPlayer player) {
        if (this.warmupService.cancel(player)) {
            this.messageProviderService.sendMessageTo(player, "warmup.cancel");
        }
    }

    public void onReload(final INucleusServiceCollection collection) {
        this.warmupConfig = collection.configProvider().getModuleConfig(CoreConfig.class).getWarmupConfig();
    }
}
