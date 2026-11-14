/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;

@EssentialsEquivalent({"hat", "head"})
@Command(
        aliases = {"hat", "head"},
        basePermission = FunPermissions.BASE_HAT,
        commandDescriptionKey = "hat",
        associatedPermissions = FunPermissions.OTHERS_HAT
)
public class HatCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(FunPermissions.OTHERS_HAT)
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer pl = context.getPlayerFromArgs();
        final boolean isSelf = context.is(pl);
        final ItemStack helment = pl.head();

        final ItemStack stack = pl.itemInHand(HandTypes.MAIN_HAND);
        if (stack.isEmpty()) {
            return context.errorResult("command.generalerror.handempty");
        }
        final ItemStack hand = stack.copy();
        hand.setQuantity(1);
        pl.setHead(hand);
        final Component itemName = hand.type().asComponent();

        final GameMode gameMode = pl.get(Keys.GAME_MODE).orElseGet(GameModes.SURVIVAL);
        if (gameMode != GameModes.CREATIVE.get()) {
            if (stack.quantity() > 1) {
                stack.setQuantity(stack.quantity() - 1);
                pl.setItemInHand(HandTypes.MAIN_HAND, stack);
            } else {
                pl.setItemInHand(HandTypes.MAIN_HAND, null);
            }
        }

        // If the old item can't be placed back in the subject inventory, drop the item.
        if (!helment.isEmpty()) {
            Util.getStandardInventory(pl).offer(helment)
                    .rejectedItems().forEach(x -> Util.dropItemOnFloorAtLocation(x, pl.serverLocation().world(),
                        pl.position()));
        }

        if (!isSelf) {
            context.sendMessage(
                    "command.hat.success",
                    context.getServiceCollection().playerDisplayNameService().getDisplayName(pl.uniqueId()),
                    itemName);
        }

        context.sendMessageTo(pl, "command.hat.successself", itemName);
        return context.successResult();
    }
}