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
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OfflineWorldParameter implements ValueParameter<ResourceKey> {

    private final Function<ResourceKey, Component> errorMessageGenerator;

    public OfflineWorldParameter(final Function<ResourceKey, Component> errorMessageGenerator) {
        this.errorMessageGenerator = errorMessageGenerator;
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String input) {
        return WorldKeys.complete(Sponge.server().worldManager().offlineWorldKeys(), input);
    }

    @Override
    public Optional<? extends ResourceKey> parseValue(final Parameter.Key<? super ResourceKey> parameterKey,
            final ArgumentReader.Mutable reader, final CommandContext.Builder context) throws ArgumentParseException {
        // By full key or, when unique, by name (see WorldKeys)
        return Optional.of(WorldKeys.read(reader, Sponge.server().worldManager().offlineWorldKeys(),
                typed -> this.errorMessageGenerator.apply(ResourceKey.resolve(typed.contains(":") ? typed : "minecraft:" + typed))));
    }

    @Override
    public List<ClientCompletionType> clientCompletionType() {
        return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
    }

}
