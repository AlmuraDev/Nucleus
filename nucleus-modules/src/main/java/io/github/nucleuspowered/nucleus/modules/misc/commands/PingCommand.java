/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@EssentialsEquivalent(value = { "ping", "pong", "echo" }, isExact = false, notes = "Returns your latency, not your message.")
@Command(
        aliases = { "ping" },
        basePermission = MiscPermissions.BASE_PING,
        commandDescriptionKey = "ping",
        associatedPermissions = MiscPermissions.OTHERS_PING
)
public class PingCommand implements ICommandExecutor { // extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier()
                        .createOnlyOtherPlayerPermissionElement(MiscPermissions.OTHERS_PING)
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer player = context.getPlayerFromArgs();
        if (context.is(player)) {
            context.sendMessage("command.ping.current.self", Util.latency(player));
        } else {
            context.sendMessage("command.ping.current.other", player.name(), Util.latency(player));
        }

        return context.successResult();
    }
}
