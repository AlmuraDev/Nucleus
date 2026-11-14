/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.PlayerChatEvent;

import java.util.Optional;

public class ChatChannelListener implements ListenerBase {

    private final IChatMessageFormatterService chatMessageFormatter;

    @Inject
    public ChatChannelListener(final INucleusServiceCollection serviceCollection) {
        this.chatMessageFormatter = serviceCollection.chatMessageFormatter();
    }

    @Listener(order = Order.LATE)
    public void onChatMessageLast(final PlayerChatEvent.Submit chat, @Root final SystemSubject source) {
        final Optional<IChatMessageFormatterService.Channel> channelOptional =
                this.chatMessageFormatter.getNucleusChannel(Util.CONSOLE_FAKE_UUID);
        channelOptional.ifPresent(x -> this.onChatChannel(chat, x));
    }

    // Used to perform any transformations so that they can be caught by other plugins.
    @Listener(order = Order.LATE)
    public void onChatMessageLast(final PlayerChatEvent.Submit chat, @Root final Player source) {
        final Optional<IChatMessageFormatterService.Channel> channelOptional =
                this.chatMessageFormatter.getNucleusChannel(source.uniqueId());
        channelOptional.ifPresent(x -> this.onChatChannel(chat, x));

    }

    private void onChatChannel(final PlayerChatEvent.Submit chat, final IChatMessageFormatterService.Channel channel) {
        if (channel.willFormat()) {
            channel.formatMessageEvent(chat.cause().first(ServerPlayer.class).get(), chat);
        }
    }

}
