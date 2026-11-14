/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.message.AudienceMessageEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class ConnectionMessagesForceListener implements ListenerBase.Conditional {

    @Listener(order = Order.LAST)
    @Include({ServerSideConnectionEvent.Leave.class, ServerSideConnectionEvent.Join.class})
    public void onPlayerLogin(final AudienceMessageEvent joinEvent) {
        joinEvent.setAudience(Sponge.server());
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(ConnectionMessagesConfig.class).isForceForAll();
    }
}
