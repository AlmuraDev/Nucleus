/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.util;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.ChatType;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryTypes;

/**
 * The {@code nucleus:chat} chat type: sender followed by content, with no brackets or separator. It is defined in
 * {@code data/nucleus/chat_type/chat.json}, which the server loads as part of the plugin's data pack. Vanilla's
 * {@code minecraft:chat} renders {@code <sender> message}, which wrapped Nucleus' formatted chat in brackets.
 */
public final class NucleusChatType {

    private static final RegistryKey<ChatType> KEY = RegistryKey.of(RegistryTypes.CHAT_TYPE, ResourceKey.of("nucleus", "chat"));

    private NucleusChatType() {
    }

    /** Whether the data pack provided the type; a message with an unknown chat type can't be sent. */
    public static boolean isLoaded() {
        return Sponge.server().registry(RegistryTypes.CHAT_TYPE).findValue(NucleusChatType.KEY.location()).isPresent();
    }

    /**
     * Shows {@code sender} then the event's message exactly as given.
     *
     * @return false if the type isn't loaded, in which case the event is left using its current type
     */
    public static boolean apply(final PlayerChatEvent.Submit event, final Component sender) {
        if (!NucleusChatType.isLoaded()) {
            return false;
        }
        event.setChatType(NucleusChatType.KEY.asReference());
        event.setSender(sender);
        return true;
    }
}
