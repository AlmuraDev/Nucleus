/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;
import io.github.nucleuspowered.nucleus.modules.mail.MailPermissions;
import io.github.nucleuspowered.nucleus.modules.mail.parameter.MailFilterArgument;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
@EssentialsEquivalent({"mail", "email"})
@Command(
        aliases = { "mail", "email" },
        basePermission = MailPermissions.BASE_MAIL,
        commandDescriptionKey = "mail",
        async = true
)
public class MailCommand implements ICommandExecutor {

    @Override
    public CommandElement[] parameters(final INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.optional(
                        GenericArguments.allOf(
                                new MailFilterArgument(Text.of(MailReadBase.FILTERS), serviceCollection.getServiceUnchecked(MailHandler.class))
                        )
                )
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        return MailReadBase.INSTANCE.executeCommand(
                context,
                context.getIfPlayer(),
                context.getAll(MailReadBase.FILTERS, NucleusMailService.MailFilter.class));
    }
}
