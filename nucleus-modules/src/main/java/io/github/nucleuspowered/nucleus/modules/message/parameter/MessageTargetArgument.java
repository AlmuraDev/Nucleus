/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.parameter;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.message.services.MessageHandler;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class MessageTargetArgument extends CommandElement {

    private final MessageHandler messageHandler;

    public MessageTargetArgument(final MessageHandler messageHandler,  @Nullable final TextComponent key) {
        super(key);
        this.messageHandler = messageHandler;
    }

    @Nullable @Override protected Object parseValue(final CommandSource source, final CommandArgs args) throws ArgumentParseException {
        return this.messageHandler.getTarget(args.next().toLowerCase()).orElseThrow(() -> args.createError(Text.of("No bot exists with that name")));
    }

    @Override public List<String> complete(final CommandSource src, final CommandArgs args, final CommandContext context) {
        final List<String> m = Lists.newArrayList(this.messageHandler.getTargetNames().keySet());
        try {
            final String a = args.peek().toLowerCase();
            return m.stream().filter(x -> x.startsWith(a)).collect(Collectors.toList());
        } catch (final ArgumentParseException e) {
            return m;
        }
    }
}
