/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.commands;

import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.UUID;

@Command(
        aliases = {"msgtoggle", "messagetoggle", "mtoggle"},
        basePermission = MessagePermissions.BASE_MSGTOGGLE,
        commandDescriptionKey = "msgtoggle",
        associatedPermissions = {
                MessagePermissions.MSGTOGGLE_BYPASS
        }
)
public class MsgToggleCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        // The core preference service: asking the module-service registry for it threw NoSuchElementException, so
        // /msgtoggle failed for everyone
        final IUserPreferenceService userPreferenceService = context.getServiceCollection().userPreferenceService();
        final UUID player = context.getIfPlayer().uniqueId();
        final NucleusUserPreferenceService.PreferenceKey<Boolean> key =
                context.getServiceCollection().userPreferenceService().keys().messageReceivingEnabled().orElseThrow(IllegalStateException::new);
        final boolean flip = context.getOne(NucleusParameters.OPTIONAL_ONE_TRUE_FALSE)
                .orElseGet(() -> userPreferenceService.getUnwrapped(player, key));

        userPreferenceService.set(player, key, flip);
        context.sendMessage("command.msgtoggle.success." + flip);

        return context.successResult();
    }
}
