/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command.parameter;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionType;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionTypes;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A loaded world, by full key or (when unique) by name; see {@link WorldKeys}.
 */
public final class OnlineWorldParameter implements ValueParameter<ServerWorld> {

    private static Collection<ResourceKey> loadedWorlds() {
        return Sponge.server().worldManager().worlds().stream().map(ServerWorld::key).collect(Collectors.toList());
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String input) {
        return WorldKeys.complete(OnlineWorldParameter.loadedWorlds(), input);
    }

    @Override
    public Optional<? extends ServerWorld> parseValue(final Parameter.Key<? super ServerWorld> parameterKey,
            final ArgumentReader.Mutable reader, final CommandContext.Builder context) throws ArgumentParseException {
        final ResourceKey key = WorldKeys.read(reader, OnlineWorldParameter.loadedWorlds(),
                typed -> Component.text("No loaded world is called \"" + typed + "\"."));
        return Sponge.server().worldManager().world(key);
    }

    @Override
    public List<ClientCompletionType> clientCompletionType() {
        return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
    }

}
