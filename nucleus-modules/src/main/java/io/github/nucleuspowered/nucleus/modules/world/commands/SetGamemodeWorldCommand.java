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
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.server.ServerWorld;

@Command(
        aliases = {"setgamemode", "setgm", "gamemode", "gm"},
        basePermission = WorldPermissions.BASE_WORLD_SETGAMEMODE,
        commandDescriptionKey = "world.setgamemode",
        parentCommand = WorldCommand.class,
        associatedPermissions = WorldPermissions.WORLD_FORCE_GAMEMODE_OVERRIDE
)
public class SetGamemodeWorldCommand implements ICommandExecutor {

    private final Parameter.Value<GameMode> gameMode =
            NucleusParameters.gameMode("gamemode").build();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.gameMode,
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final GameMode gamemodeInput = context.requireOne(this.gameMode);
        final ServerWorld worldProperties = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL)
                .orElseThrow(() -> context.createException("command.world.player"));

        worldProperties.properties().setGameMode(gamemodeInput);
        context.sendMessage("command.world.setgamemode.success",
            worldProperties.key().asString(),
            gamemodeInput.asComponent());

        return context.successResult();
    }
}
