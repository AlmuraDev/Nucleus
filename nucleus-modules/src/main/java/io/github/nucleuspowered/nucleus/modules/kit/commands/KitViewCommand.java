/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.KitUtil;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.List;
import java.util.stream.Collectors;

@Command(
        aliases = { "view" },
        basePermission = KitPermissions.BASE_KIT_VIEW,
        commandDescriptionKey = "kit.view",
        parentCommand = KitCommand.class
)
public class KitViewCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean processTokens = false;

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(KitService.class).kitParameterWithPermission()
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer src = context.requirePlayer();
        final Kit kitInfo = context.requireOne(KitService.KIT_KEY);
        final KitService service = context.getServiceCollection().getServiceUnchecked(KitService.class);

        final ViewableInventory kitInv = KitUtil.getKitInventoryBuilder();
        kitInfo.getStacks().stream().filter(x -> !x.isEmpty()).forEach(x -> kitInv.offer(x.asMutableCopy()));
        final InventoryMenu inventory = kitInv.asMenu();
        inventory.setTitle(context.getMessage("command.kit.edit.title", kitInfo.getName()));
        inventory.setReadOnly(true);

        final List<ItemStack> lis = kitInfo.getStacks().stream().filter(x -> !x.isEmpty()).map(ItemStackSnapshot::asMutableCopy)
                .collect(Collectors.toList());
        if (this.processTokens) {
            service.processTokensInItemStacks(src, lis);
        }

        lis.forEach(kitInv::offer);
        return inventory.open(src).map(x -> context.successResult())
                .orElseGet(() -> context.errorResult("command.kit.view.cantopen", kitInfo.getName()));
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.processTokens = serviceCollection.configProvider().getModuleConfig(KitConfig.class).isProcessTokens();
    }
}
