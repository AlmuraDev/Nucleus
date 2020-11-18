/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands.itemname;

import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.command.exception.CommandException;

@Command(aliases = "itemname", basePermission = ItemPermissions.BASE_ITEMNAME, commandDescriptionKey = "itemname", hasExecutor = false)
public class ItemNameCommand implements ICommandExecutor {

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        return context.failResult();
    }
}
