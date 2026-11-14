/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.core.services.impl.chatmessageformatter.ChatMessageFormatterService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.message.PlayerChatEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@ImplementedBy(ChatMessageFormatterService.class)
public interface IChatMessageFormatterService {

    Optional<Channel> getNucleusChannel(Audience source);

    Optional<Channel> getNucleusChannel(UUID uuid);

    void setPlayerNucleusChannel(UUID uuid, @Nullable Channel channel);

    NoExceptionAutoClosable setAudienceNucleusChannelTemporarily(Audience source, Channel channel);

    NoExceptionAutoClosable setPlayerNucleusChannelTemporarily(UUID uuid, Channel channel);

    interface Channel {

        default boolean willFormat() {
            return false;
        }

        default Audience receivers() {
            return Sponge.server();
        }

        default boolean ignoreIgnoreList() {
            return false;
        }

        Component formatMessage(Audience source, Component body);

        /**
         * Which players receive a chat message sent on this channel, or empty for everyone. Chat can no longer be
         * redirected to an arbitrary audience, only filtered, so this must agree with {@link #receivers()}.
         */
        default Optional<Predicate<ServerPlayer>> recipientFilter() {
            return Optional.empty();
        }

        default void formatMessageEvent(final Audience audience, final PlayerChatEvent.Submit event) {
            this.recipientFilter().ifPresent(event::setFilter);
            event.setMessage(this.formatMessage(audience, event.message()));
        }

    }

}
