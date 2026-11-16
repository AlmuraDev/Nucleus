/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.gamerule.GameRule;
import org.spongepowered.api.world.server.ServerWorld;

import java.lang.reflect.Type;

@Command(
        aliases = {"set"},
        basePermission = WorldPermissions.BASE_WORLD_GAMERULE_SET,
        commandDescriptionKey = "world.gamerule.set",
        parentCommand = GameruleCommand.class
)
public class SetGameruleCommand implements ICommandExecutor {

    private final Parameter.Value<GameRule<?>> gameMode = Parameter.builder(new TypeToken<GameRule<?>>() {})
            .addParser(VariableValueParameters.registryEntryBuilder(RegistryTypes.GAME_RULE).defaultNamespace("minecraft").build())
            .key("gameRule")
            .build();

    private final Parameter.Value<Integer> intVal = Parameter.integerNumber().key("integer").build();
    private final Parameter.Value<String> stringVal = Parameter.string().key("string").build();

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONLINE_WORLD_OPTIONAL,
                this.gameMode,
                Parameter.firstOf(
                        NucleusParameters.ONE_TRUE_FALSE,
                        this.intVal,
                        this.stringVal
                )
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerWorld serverWorld = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD.key())
                .orElseThrow(() -> context.createException("command.world.player"));
        final GameRule<?> gameRule = context.requireOne(this.gameMode);
        final Type type = gameRule.valueType();
        if (type == Boolean.class || type == boolean.class) {
            final boolean val = context.getOne(NucleusParameters.ONE_TRUE_FALSE)
                    .orElseThrow(() -> context.createException("command.world.gamerule.set.boolean"));
            serverWorld.properties().setGameRule((GameRule<Boolean>) gameRule, val);
            context.sendMessage("command.world.gamerule.set.success", gameRule, val, serverWorld.key().asString());
        } else if (type == Integer.class || type == int.class) {
            final int val = context.getOne(this.intVal)
                    .orElseThrow(() -> context.createException("command.world.gamerule.set.integer"));
            serverWorld.properties().setGameRule((GameRule<Integer>) gameRule, val);
            context.sendMessage("command.world.gamerule.set.success", gameRule, val, serverWorld.key().asString());
        } else if (type == String.class) {
            final String val = context.getOne(this.stringVal)
                    .orElseGet(() -> context.getOne(this.intVal)
                            .map(String::valueOf)
                            .orElseGet(() -> String.valueOf(context.requireOne(NucleusParameters.ONE_TRUE_FALSE))));
            serverWorld.properties().setGameRule((GameRule<String>) gameRule, val);
            context.sendMessage("command.world.gamerule.set.success", gameRule, val, serverWorld.key().asString());
        } else {
            return context.errorResult("command.world.gamerule.set.notype");
        }
        return context.successResult();
    }

}
