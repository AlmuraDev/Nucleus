/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.KitUtil;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.Optional;

@Command(
        aliases = { "edit", "ed" },
        basePermission = KitPermissions.BASE_KIT_EDIT,
        commandDescriptionKey = "kit.edit",
        parentCommand = KitCommand.class
)
public class KitEditCommand implements ICommandExecutor {

    private final IMessageProviderService messageProviderService;
    private final Cache<String, Container> openContainers = Caffeine.newBuilder()
            .weakValues()
            .build();

    @Inject
    public KitEditCommand(final IMessageProviderService messageProviderService) {
        this.messageProviderService = messageProviderService;
    }

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(KitService.class).kitParameterWithoutPermission()
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer player = context.requirePlayer();
        final KitService service = context.getServiceCollection().getServiceUnchecked(KitService.class);
        final Kit kitInfo = context.requireOne(KitService.KIT_KEY);

        final String kitName = kitInfo.getName();
        if (this.openContainers.getIfPresent(kitName) != null) {
            return context.errorResult("command.kit.edit.current", kitInfo.getName());
        }

        final ViewableInventory kitInv = KitUtil.getKitInventoryBuilder();
        kitInfo.getStacks().stream().filter(x -> !x.isEmpty()).forEach(x -> kitInv.offer(x.asMutableCopy()));
        final InventoryMenu inventory = kitInv.asMenu();
        inventory.setTitle(context.getMessage("command.kit.edit.title", kitName));
        inventory.registerClose((cause, container) -> {
            final Optional<Kit> kit = service.getKit(kitName);
            if (kit.isPresent()) {
                service.saveKit(kit.get().updateKitInventory(inventory.inventory()));
                this.messageProviderService.sendMessageTo(player, "command.kit.edit.success", kitName);
            } else {
                this.messageProviderService.sendMessageTo(player, "command.kit.edit.error", kitName);
            }
            this.openContainers.invalidate(kitName);
        });
        final Container container = inventory.open(player).orElseThrow(() -> context.createException("command.kit.edit.cantopen", kitInfo.getName()));
        this.openContainers.put(kitName, container);
        return context.successResult();
    }
}
