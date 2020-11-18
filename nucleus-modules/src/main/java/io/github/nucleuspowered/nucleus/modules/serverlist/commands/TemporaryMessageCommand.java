/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.commands;

import io.github.nucleuspowered.nucleus.modules.serverlist.ServerListPermissions;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.BoundedIntegerArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Command(
        aliases = { "message", "m" },
        basePermission = ServerListPermissions.BASE_SERVERLIST_MESSAGE,
        commandDescriptionKey = "serverlist.message",
        parentCommand = ServerListCommand.class
)
public class TemporaryMessageCommand implements ICommandExecutor {

    private final String line = "line";

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            GenericArguments.flags()
                .flag("r", "-remove")
                .valueFlag(new BoundedIntegerArgument(Text.of(this.line), 1, 2, serviceCollection),"l", "-line")
                .valueFlag(NucleusParameters.DURATION.get(serviceCollection), "t", "-time")
                .buildWith(NucleusParameters.OPTIONAL_MESSAGE)
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        // Get the temporary message item.
        final ServerListService mod = context.getServiceCollection().getServiceUnchecked(ServerListService.class);

        if (context.hasAny("r")) {
            if (mod.getMessage().isPresent()) {
                // Remove
                mod.clearMessage();

                // Send message.
                context.sendMessage("command.serverlist.message.removed");
                return context.successResult();
            }

            return context.errorResult("command.serverlist.message.noremoved");
        }

        // Which line?
        final boolean linetwo = context.getOne(this.line, Integer.class).map(x -> x == 2).orElse(false);

        final Optional<String> onMessage = context.getOne(NucleusParameters.Keys.MESSAGE, String.class);

        if (!onMessage.isPresent()) {
            final boolean isValid = mod.getExpiry().map(x -> x.isAfter(Instant.now())).orElse(false);
            if (!isValid) {
                return context.errorResult("command.serverlist.message.isempty");
            }

            if (linetwo) {
                mod.updateLineTwo(null);
            } else {
                mod.updateLineOne(null);
            }

            final Optional<Text> newMessage = mod.getMessage();

            if (newMessage.isPresent()) {
                // Send message
                context.sendMessage("command.serverlist.message.set");
                context.sendMessageText(newMessage.get());
            } else {
                context.sendMessage("command.serverlist.message.empty");
            }

            return context.successResult();
        }

        final String nMessage = onMessage.get();

        // If the expiry is null or before now, and there is no timespan, then it's an hour.
        final Instant endTime = context.getOne(NucleusParameters.Keys.DURATION, Long.class).map(x -> Instant.now().plus(x, ChronoUnit.SECONDS))
                .orElseGet(() -> mod.getExpiry().map(x -> x.isBefore(Instant.now()) ? x.plusSeconds(3600) : x)
                .orElseGet(() -> Instant.now().plusSeconds(3600)));

        // Set the expiry.
        if (linetwo) {
            mod.setMessage(null, nMessage, endTime);
        } else {
            mod.setMessage(nMessage, null, endTime);
        }

        final Optional<Text> newMessage = mod.getMessage();

        if (newMessage.isPresent()) {
            // Send message
            context.sendMessage("command.serverlist.message.set");
            context.sendMessageText(newMessage.get());
            context.sendMessage("command.serverlist.message.expiry", context.getTimeToNowString(endTime));
            return context.successResult();
        }


        return context.errorResult("command.serverlist.message.notset");
    }
}
