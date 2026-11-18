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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Any world the server knows about, loaded or not, for commands that load the world when they need it.
 */
public class KnownWorldParameter implements ValueParameter<ResourceKey> {

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String input) {
        return WorldKeys.complete(Sponge.server().worldManager().worldKeys(), input);
    }

    @Override
    public Optional<? extends ResourceKey> parseValue(final Parameter.Key<? super ResourceKey> parameterKey,
            final ArgumentReader.Mutable reader, final CommandContext.Builder context) throws ArgumentParseException {
        return Optional.of(WorldKeys.read(reader, Sponge.server().worldManager().worldKeys(),
                typed -> Component.text("No world is called \"" + typed + "\".")));
    }

    @Override
    public List<ClientCompletionType> clientCompletionType() {
        return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
    }

}
