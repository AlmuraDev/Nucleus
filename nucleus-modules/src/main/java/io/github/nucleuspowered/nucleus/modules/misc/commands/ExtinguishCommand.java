/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Ticks;

@Command(
        aliases = {"extinguish", "ext"},
        basePermission = MiscPermissions.BASE_EXTINGUISH,
        commandDescriptionKey = "extinguish",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MiscPermissions.EXEMPT_COOLDOWN_EXTINGUISH),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MiscPermissions.EXEMPT_WARMUP_EXTINGUISH),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MiscPermissions.EXEMPT_COST_EXTINGUISH)
        },
        associatedPermissions = MiscPermissions.OTHERS_EXTINGUISH
)
public class ExtinguishCommand implements ICommandExecutor {

    private final Parameter.Value<ServerPlayer> parameter;

    @Inject
    public ExtinguishCommand(final INucleusServiceCollection serviceCollection) {
        this.parameter = serviceCollection.commandElementSupplier()
                .createOnlyOtherPlayerPermissionElement(MiscPermissions.OTHERS_EXTINGUISH);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.parameter
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer target = context.getPlayerFromArgs();
                // this.getUserFromArgs(Player.class, src, NucleusParameters.Keys.PLAYER, args);
        final Ticks ticks = target.get(Keys.FIRE_TICKS).orElseGet(Ticks::zero);
        if (ticks.ticks() > 0 && target.offer(Keys.FIRE_TICKS, Ticks.zero()).isSuccessful()) {
            context.sendMessage("command.extinguish.success", target.name());
            return context.successResult();
        }

        return context.errorResult("command.extinguish.fail", target.name());
    }
}
