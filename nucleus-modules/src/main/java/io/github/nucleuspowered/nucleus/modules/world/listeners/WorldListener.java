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
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class WorldListener implements ListenerBase.Conditional {

    private final Set<UUID> messageSent = new HashSet<>();
    private final INucleusServiceCollection serviceCollection;

    @Inject
    public WorldListener(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener(order = Order.LAST)
    public void onPlayerTeleport(final ChangeEntityWorldEvent.Pre event, @Getter("getEntity") final ServerPlayer player) {
        final ServerWorld target = event.getOriginalWorld();
        if (player.getWorld().equals(target)) {
            // I mean, this is a change world event, but you never know.
            return;
        }

        final IPermissionService permissionService = this.serviceCollection.permissionService();
        if (!permissionService.isConsoleOverride(event.getCause().first(Subject.class).orElse(player)) &&
                !this.serviceCollection.permissionService().hasPermission(player, WorldPermissions.getWorldAccessPermission(target.getKey().asString()))) {
            event.setCancelled(true);
            if (!this.messageSent.contains(player.getUniqueId())) {
                this.serviceCollection.messageProvider().sendMessageTo(player, "world.access.denied", target.getKey().asString());
            }

            this.messageSent.add(player.getUniqueId());
            Sponge.getServer().getScheduler().submit(
                    Task.builder()
                            .delay(Ticks.of(1))
                            .execute(this.relocate(player))
                            .plugin(this.serviceCollection.pluginContainer())
                            .build());
        }
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return this.serviceCollection.configProvider().getModuleConfig(WorldConfig.class).isSeparatePermissions();
    }

    private Consumer<ScheduledTask> relocate(final ServerPlayer player) {
        return task -> {
            final Optional<ServerLocation> location = Sponge.getServer()
                    .getTeleportHelper()
                    .getSafeLocationWithBlacklist(player.getServerLocation(), 5, 5, 5, TeleportHelperFilters.NO_PORTAL.get());
            if (location.isPresent()) {
                player.setLocation(location.get());
            } else {
                player.setPosition(player.getWorld().getProperties().getSpawnPosition().toDouble());
            }

            this.messageSent.remove(player.getUniqueId());
        };
    }

}
