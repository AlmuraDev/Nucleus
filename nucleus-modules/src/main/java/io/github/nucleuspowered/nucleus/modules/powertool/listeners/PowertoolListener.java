/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolKeys;
import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolPermissions;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.item.ItemType;

public class PowertoolListener implements ListenerBase {

    private final PowertoolService service;
    private final IUserPreferenceService userPreferenceService;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;

    @Inject
    public PowertoolListener(final INucleusServiceCollection serviceCollection) {
        this.service = serviceCollection.getServiceUnchecked(PowertoolService.class);
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener
    public void onLogout(final ServerSideConnectionEvent.Leave event) {
        this.service.reset(event.player().uniqueId());
    }

    @Listener
    public void onUserInteract(final InteractEvent event, @Root final ServerPlayer player) {
        if (event instanceof InteractBlockEvent || !(event instanceof Cancellable)) {
            return;
        }
        // No item in hand or no permission -> no powertool.
        if (player.itemInHand(HandTypes.MAIN_HAND).isEmpty() ||
                !this.permissionService.hasPermission(player, PowertoolPermissions.BASE_POWERTOOL)) {
            return;
        }

        // Get the item and the user.
        final ItemType item = player.itemInHand(HandTypes.MAIN_HAND).type();

        // If the powertools are toggled on.
        if (this.userPreferenceService.get(player.uniqueId(), PowertoolKeys.POWERTOOL_ENABLED).orElse(true)) {
            // Execute all powertools if they exist.
            this.service.getPowertoolForItem(player.uniqueId(), item).ifPresent(x -> {
                // Cancel the interaction.
                ((Cancellable) event).setCancelled(true);

                final Player interacting;
                if (event instanceof InteractEntityEvent && ((InteractEntityEvent) event).entity() instanceof ServerPlayer) {
                    interacting = (Player) ((InteractEntityEvent) event).entity();
                } else {
                    interacting = null;
                }

                // Run each command.
                if (interacting == null && x.stream().allMatch(i -> i.contains("{{subject}}"))) {
                    this.messageProviderService.sendMessageTo(player, "powertool.playeronly");
                    return;
                }

                x.forEach(s -> {
                    if (s.contains("{{subject}}")) {
                        if (interacting != null) {
                            s = s.replace("{{subject}}", interacting.name());
                        } else {
                            // Don't execute when no subject is in the way.
                            return;
                        }
                    }

                    try {
                        Sponge.server().commandManager().process(s);
                    } catch (final CommandException e) {
                        // ignored
                    }
                });
            });
        }
    }
}
