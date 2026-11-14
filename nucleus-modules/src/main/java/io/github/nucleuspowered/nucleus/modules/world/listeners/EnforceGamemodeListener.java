/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashSet;
import java.util.Set;

public class EnforceGamemodeListener implements ListenerBase.Conditional {

    private final PluginContainer pluginContainer;

    @Inject
    public EnforceGamemodeListener(final INucleusServiceCollection serviceCollection) {
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener(order = Order.POST)
    public void onPlayerLogin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        Sponge.server().scheduler().submit(
                Task.builder().execute(() -> this.enforce(player, player.world())).plugin(this.pluginContainer).build()
        );
    }

    @Listener(order = Order.POST)
    public void onPlayerTeleport(final ChangeEntityWorldEvent.Post event,
            @Getter("entity") final ServerPlayer player,
            @Getter("originalWorld") final ServerWorld from,
            @Getter("destinationWorld") final ServerWorld to) {
        if (!from.equals(to)) {
            this.enforce(player, to);
        }
    }

    @Listener(order = Order.POST)
    public void onPlayerTeleport(final RespawnPlayerEvent.Post event,
            @Getter("entity") final ServerPlayer player,
            @Getter("originalWorld") final ServerWorld from,
            @Getter("destinationWorld") final ServerWorld to) {
        if (!from.equals(to)) {
            this.enforce(player, to);
        }
    }

    private void enforce(final ServerPlayer player, final ServerWorld world) {
        final Set<Context> contextSet = Sponge.server().serviceProvider().contextService().contextsFor(player.contextCause());
        contextSet.removeIf(x -> x.getKey().equals(Context.WORLD_KEY));
        contextSet.add(new Context(Context.WORLD_KEY, world.key().asString()));
        if (!player.hasPermission(WorldPermissions.WORLD_FORCE_GAMEMODE_OVERRIDE)) {
            // set their gamemode accordingly.
            player.offer(Keys.GAME_MODE, world.properties().gameMode());
        }
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(WorldConfig.class).isEnforceGamemodeOnWorldChange();
    }

}
