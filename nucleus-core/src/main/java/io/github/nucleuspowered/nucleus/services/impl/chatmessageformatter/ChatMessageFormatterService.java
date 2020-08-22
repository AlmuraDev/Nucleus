/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.chatmessageformatter;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Singleton;

@Singleton
public class ChatMessageFormatterService implements IChatMessageFormatterService {

    private final Map<UUID, Channel> chatChannels = new HashMap<>();

    @Override
    public Optional<Channel> getNucleusChannel(final UUID uuid) {
        return Optional.ofNullable(this.chatChannels.get(uuid));
    }

    @Override
    public void setPlayerNucleusChannel(final UUID uuid, @Nullable final Channel channel) {
        if (channel == null) {
            this.chatChannels.remove(uuid);
        } else {
            this.chatChannels.put(uuid, channel);
        }
    }

    @Override
    public NoExceptionAutoClosable setPlayerNucleusChannelTemporarily(final UUID uuid, final Channel channel) {
        Preconditions.checkNotNull(channel);
        final Channel original = this.chatChannels.get(uuid);
        final Optional<Player> player = Sponge.getServer().getPlayer(uuid);
        final MessageChannel originalChannel = player.map(MessageReceiver::getMessageChannel).orElse(null);
        this.chatChannels.put(uuid, channel);
        if (channel instanceof Channel.External<?>) {
            ((Channel.External<?>) channel).createChannel(originalChannel);
        }
        return () -> {
            this.setPlayerNucleusChannel(uuid, original);
            if (originalChannel != null) {
                player.ifPresent(x -> x.setMessageChannel(originalChannel));
            }
        };
    }

}
