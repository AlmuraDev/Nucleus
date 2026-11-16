/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;

@EssentialsEquivalent(value = {"time"}, isExact = false, notes = "This just displays the time. Use '/settime' to set the time.")
@Command(
        aliases = {"gettime", "$time"},
        basePermission = EnvironmentPermissions.BASE_TIME,
        commandDescriptionKey = "time",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = EnvironmentPermissions.EXEMPT_COOLDOWN_TIME),
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission =  EnvironmentPermissions.EXEMPT_WARMUP_TIME),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = EnvironmentPermissions.EXEMPT_COST_TIME)
        }
)
public class TimeCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) {
        final ServerWorld pr = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL.key())
                .orElseGet(() -> Util.defaultWorld());

        // The time of day, as /settime shows it (asInGameDuration is how long the world has existed, e.g. "1 day, 6 hours")
        context.sendMessage("command.time", pr.key().asString(),
                Util.getTimeFromDayTime(context.getServiceCollection().messageProvider(), pr.properties().dayTime()));
        return context.successResult();
    }
}
