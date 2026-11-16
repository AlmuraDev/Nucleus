/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.experience.parameter;

import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionTypes;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionType;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes an argument of the form "l30" or "lv30" or "l:30" or "lv:30". Returns
 * the integer.
 */
public class ExperienceLevelArgument implements ValueParameter<Integer> {

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("^(l|lv|l:|lv:)?(\\d+)(l|lv)?$", Pattern.CASE_INSENSITIVE);
    private final IMessageProviderService messageProviderService;

    public ExperienceLevelArgument(final INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Override
    public Optional<? extends Integer> parseValue(
            final Parameter.Key<? super Integer> parameterKey,
            final ArgumentReader.Mutable reader,
            final CommandContext.Builder context) throws ArgumentParseException {
        final String nextElement = reader.parseString();
        final Matcher m = ExperienceLevelArgument.ARGUMENT_PATTERN.matcher(nextElement);
        if (m.find(0) && (m.group(1) != null || m.group(3) != null)) {
            return Optional.of(Integer.parseInt(m.group(2)));
        }

        throw reader.createException(this.messageProviderService.getMessageFor(context.cause().audience(), "args.explevel.error"));
    }

    // A word such as "10L": an integer argument type made the client reject it
    @Override
    public List<ClientCompletionType> clientCompletionType() {
        return Collections.singletonList(ClientCompletionTypes.STRING.get());
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
        return Collections.emptyList();
    }
}
