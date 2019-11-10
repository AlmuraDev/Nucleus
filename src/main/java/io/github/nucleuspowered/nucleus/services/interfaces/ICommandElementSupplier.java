/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.command.ICommandContext;
import io.github.nucleuspowered.nucleus.command.parameter.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.services.impl.commandelement.CommandElementSupplier;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;

@ImplementedBy(CommandElementSupplier.class)
public interface ICommandElementSupplier {

    CommandElement createOnlyOtherUserPermissionElement(String permission);

    CommandElement createOtherUserPermissionElement(boolean isPlayer, String permission);

    CommandElement createOnlyOtherUserPermissionElement(boolean isPlayer, String permission);

    NucleusRequirePermissionArgument createPermissionParameter(CommandElement wrapped, String permission);

    User getUserFromParametersElseSelf(ICommandContext<? extends CommandSource> context) throws CommandException;
}
