/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

@Command(
        aliases = {"load"},
        basePermission = WorldPermissions.BASE_WORLD_LOAD,
        commandDescriptionKey = "world.load",
        parentCommand = WorldCommand.class
)
public class LoadWorldCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            NucleusParameters.OFFLINE_WORLD
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ResourceKey targetWorld = context.requireOne(NucleusParameters.OFFLINE_WORLD);
        if (Sponge.server().worldManager().world(targetWorld).isPresent()) {
            return context.errorResult("command.world.load.alreadyloaded", targetWorld.asString());
        }

        context.sendMessage("command.world.load.start", targetWorld.asString());
        Sponge.server().worldManager().loadWorld(targetWorld).handle((world, exception) -> {
            // An empty result means the world couldn't be loaded, e.g. nothing is known about it any more
            if (exception != null || world == null || !world.isPresent()) {
                context.getServiceCollection().schedulerService().runOnMainThread(() ->
                    context.sendMessage("command.world.load.fail", targetWorld.asString()));
            } else {
                context.getServiceCollection().schedulerService().runOnMainThread(() ->
                        context.sendMessage("command.world.load.loaded", targetWorld.asString()));
            }
            return null;
        });
        return context.successResult();
    }


}
