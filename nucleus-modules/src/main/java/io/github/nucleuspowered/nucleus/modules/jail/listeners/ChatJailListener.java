/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;

import com.google.inject.Inject;
import org.spongepowered.api.event.message.PlayerChatEvent;

public class ChatJailListener implements ListenerBase.Conditional {

    private final JailService handler;
    private final IMessageProviderService messageProviderService;

    @Inject
    public ChatJailListener(final INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailService.class);
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener(order = Order.FIRST)
    public void onChat(final PlayerChatEvent.Submit event, @Root final ServerPlayer player) {
        if (this.handler.isPlayerJailed(player.uniqueId())) {
            this.messageProviderService.sendMessageTo(player, "jail.muteonchat");
            event.setCancelled(true);
        }
    }

    @Override public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(JailConfig.class).isMuteOnJail();
    }

}
