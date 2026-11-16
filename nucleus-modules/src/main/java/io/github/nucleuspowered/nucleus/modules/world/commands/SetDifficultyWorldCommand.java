/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;

@Command(
        aliases = {"setdifficulty", "difficulty"},
        basePermission = WorldPermissions.BASE_WORLD_SETDIFFICULTY,
        commandDescriptionKey = "world.setdifficulty",
        parentCommand = WorldCommand.class
)
public class SetDifficultyWorldCommand implements ICommandExecutor {

    private final Parameter.Value<Difficulty> difficultyValue =
            NucleusParameters.difficulty("difficulty").build();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.difficultyValue,
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Difficulty difficultyInput = context.requireOne(this.difficultyValue);
        final ServerWorld worldProperties = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL)
                        .orElseThrow(() -> context.createException("command.world.player"));

        worldProperties.properties().setDifficulty(difficultyInput);
        context.sendMessage("command.world.setdifficulty.success",
                worldProperties.key().asString(),
                difficultyInput.asComponent());

        return context.successResult();
    }
}
