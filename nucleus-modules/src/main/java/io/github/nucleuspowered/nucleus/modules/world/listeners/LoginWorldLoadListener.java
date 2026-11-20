/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.server.WorldManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Puts a player back in the world they logged out in when that world isn't loaded (e.g. load-on-startup is off).
 *
 * <p>Without this, the game finds no level for the saved dimension when it places the player and quietly drops them
 * into the default world at the same coordinates. Auth fires on Sponge's login threads, before the player is placed,
 * so the world is loaded here and the login waits for it. A spawn-on-login setting still applies afterwards, at
 * login.</p>
 */
public class LoginWorldLoadListener implements ListenerBase {

    private static final long LOAD_TIMEOUT_SECONDS = 60;

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public LoginWorldLoadListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener(order = Order.LATE)
    public void onAuth(final ServerSideConnectionEvent.Auth event) {
        final UUID userId = event.profile().uniqueId();
        // The world load finishes on the main thread, so waiting on it from there would never return
        if (event.isCancelled() || userId == null || Sponge.server().onMainThread()) {
            return;
        }

        final Optional<ResourceKey> savedWorld;
        try {
            // Empty for a first join: nothing saved, so the normal spawn applies
            savedWorld = Sponge.server().userManager().load(userId)
                    .get(LoginWorldLoadListener.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .map(User::worldKey);
        } catch (final Exception ex) {
            this.serviceCollection.logger().warn("Could not read the saved world of {}", userId, ex);
            return;
        }

        if (!savedWorld.isPresent()) {
            return;
        }

        final ResourceKey worldKey = savedWorld.get();
        final WorldManager worldManager = Sponge.server().worldManager();
        // Loaded already, or gone (then the game's own fallback applies)
        if (worldManager.world(worldKey).isPresent() || !worldManager.worldExists(worldKey)) {
            return;
        }

        this.serviceCollection.logger().info("Loading world {} for {}, who logged out there", worldKey.asString(),
                event.profile().name().orElse(userId.toString()));
        try {
            if (!worldManager.loadWorld(worldKey).get(LoginWorldLoadListener.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).isPresent()) {
                this.serviceCollection.logger().warn("Could not load world {}; the player will be placed in the default world",
                        worldKey.asString());
            }
        } catch (final Exception ex) {
            this.serviceCollection.logger().warn("Could not load world {}; the player will be placed in the default world",
                    worldKey.asString(), ex);
        }
    }

}
