/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.modules.mail.MailPermissions;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.exception.CommandException;;

@Command(
        aliases = { "clear" },
        basePermission = MailPermissions.BASE_MAIL,
        commandDescriptionKey = "mail.clear",
        parentCommand = MailCommand.class,
        async = true
)
public class ClearMailCommand implements ICommandExecutor {

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        if (context.getServiceCollection().getServiceUnchecked(MailHandler.class).clearUserMail(context.getIfPlayer())) {
            context.sendMessage("command.mail.clear.success");
        } else {
            context.sendMessage("command.mail.clear.nomail");
        }

        return context.successResult();
    }
}
