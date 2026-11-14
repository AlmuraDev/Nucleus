/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.listener;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.message.services.MessageHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public final class MessageListener implements ListenerBase {

    private final MessageHandler messageHandler;

    @Inject
    public MessageListener(final INucleusServiceCollection serviceCollection) {
        this.messageHandler = serviceCollection.getServiceUnchecked(MessageHandler.class);
    }

    @Listener(order = Order.LAST)
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer serverPlayer) {
        this.messageHandler.addPlayer(serverPlayer.uniqueId());
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(final ServerSideConnectionEvent.Leave event, @Getter("player") final ServerPlayer serverPlayer) {
        this.messageHandler.removePlayer(serverPlayer.uniqueId());
    }

}
