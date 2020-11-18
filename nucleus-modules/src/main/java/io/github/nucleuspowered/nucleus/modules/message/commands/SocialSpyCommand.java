/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.commands;

import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.modules.message.services.MessageHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
@EssentialsEquivalent("socialspy")
@Command(
        aliases = {"socialspy"},
        basePermission = MessagePermissions.BASE_SOCIALSPY,
        commandDescriptionKey = "socialspy",
        associatedPermissionLevelKeys = MessagePermissions.SOCIALSPY_LEVEL_KEY,
        associatedPermissions = MessagePermissions.SOCIALSPY_FORCE
)
public class SocialSpyCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Player src = context.getCommandSourceRoot();
        final MessageHandler handler = context.getServiceCollection().getServiceUnchecked(MessageHandler.class);
        if (handler.forcedSocialSpyState(src).asBoolean()) {
            return context.errorResult("command.socialspy.forced");
        }

        final boolean spy = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElseGet(() -> !handler.isSocialSpy(src));
        if (handler.setSocialSpy(src, spy)) {
            context.sendMessage(spy ? "command.socialspy.on" : "command.socialspy.off");
            return context.successResult();
        }

        return context.errorResult("command.socialspy.unable");
    }
}
