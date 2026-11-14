/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.Optional;

public class NicknameListener implements ListenerBase {

    private final NicknameService nicknameService;

    @Inject
    public NicknameListener(final INucleusServiceCollection serviceCollection) {
        this.nicknameService = serviceCollection.getServiceUnchecked(NicknameService.class);
    }

    @Listener(order = Order.FIRST)
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        final Optional<Component> nickname = this.nicknameService.getNickname(player.uniqueId());
        this.nicknameService.markRead(player.uniqueId());
        if (nickname.isPresent()) {
            this.nicknameService.updateCache(player.uniqueId(), nickname.get());
            player.offer(Keys.CUSTOM_NAME, nickname.get());
        } else {
            player.remove(Keys.CUSTOM_NAME);
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerQuit(final ServerSideConnectionEvent.Leave event, @Getter("player") final ServerPlayer player) {
        this.nicknameService.removeFromCache(player.uniqueId());
    }

}
