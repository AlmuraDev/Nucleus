/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.traits;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.argumentparsers.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.service.permission.Subject;

public interface PermissionTrait {

    default CommandPermissionHandler getPermissionHandlerFor(Class<? extends AbstractCommand<?>> clazz) {
        return Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(clazz);
    }

    default boolean hasPermission(Subject subject, String permission) {
        return Nucleus.getNucleus().getPermissionResolver().hasPermission(subject, permission);
    }

    default boolean hasPermission(Subject src, String permission, boolean resultIfOverriden) {
        if (Nucleus.getNucleus().isConsoleBypass() && src instanceof ConsoleSource) {
            return resultIfOverriden;
        }

        return hasPermission(src, permission);
    }


    default CommandElement requirePermissionArg(CommandElement wrapped, String permission) {
        return new NucleusRequirePermissionArgument(wrapped, permission);
    }

}
