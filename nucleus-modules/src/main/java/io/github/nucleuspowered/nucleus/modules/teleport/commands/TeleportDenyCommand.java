/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import org.spongepowered.api.command.exception.CommandException;

@EssentialsEquivalent({"tpdeny", "tpno"})
@Command(
        aliases = {"tpdeny", "teleportdeny", "tpno"},
        basePermission = TeleportPermissions.BASE_TPDENY,
        commandDescriptionKey = "tpdeny"
)
public class TeleportDenyCommand implements ICommandExecutor {

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        return context.getResultFromBoolean(
                context.getServiceCollection().getServiceUnchecked(PlayerTeleporterService.class).deny(context.getIfPlayer()));
    }
}
