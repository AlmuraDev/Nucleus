/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.Sponge;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.server.ServerWorld;

import java.time.Duration;

@Command(
        aliases = { "set" },
        basePermission = WorldPermissions.BASE_BORDER_SET,
        commandDescriptionKey = "world.border.set",
        parentCommand = BorderCommand.class
)
public class SetBorderCommand implements ICommandExecutor {

    private final Parameter.Value<Integer> xParam = Parameter.integerNumber().key("x").build();
    private final Parameter.Value<Integer> zParam = Parameter.integerNumber().key("z").build();
    private final Parameter.Value<Integer> diameterParameter = Parameter.rangedInteger(1, Integer.MAX_VALUE).key("diameter").build();
    private final Parameter.Value<Duration> durationParameter = Parameter.duration().key("delay").optional().build();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                Parameter.seqBuilder(NucleusParameters.ONLINE_WORLD_OPTIONAL)
                    .then(this.xParam)
                    .then(this.zParam)
                    .optional()
                    .build(),
                this.diameterParameter,
                this.durationParameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerWorld wp = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL)
                .orElseThrow(() -> context.createException("command.world.player"));
        final int x;
        final int z;
        final int dia = context.requireOne(this.diameterParameter);
        final Duration delay = context.getOne(this.durationParameter).orElse(Duration.ZERO);

        if (context.is(Locatable.class)) {
            final ServerLocation lw = ((Locatable) context.getCommandSourceRoot()).serverLocation();
            if (context.hasAny(this.zParam)) {
                x = context.requireOne(this.xParam);
                z = context.requireOne(this.zParam);
            } else {
                x = lw.blockX();
                z = lw.blockZ();
            }
        } else {
            x = context.requireOne(this.xParam);
            z = context.requireOne(this.zParam);
        }

        final WorldBorder.Builder border = wp.border().toBuilder();
        // Now, if we have an x and a z key, get the centre from that.
        border.center(x, z);

        border.targetDiameter(dia);
        if (delay == Duration.ZERO) {
            context.sendMessage("command.world.setborder.set",
                    wp.key().asString(),
                    String.valueOf(x),
                    String.valueOf(z),
                    String.valueOf(dia));
        } else {
            border.targetDiameter(dia);
            border.timeToTargetDiameter(Ticks.ofWallClockTime(Sponge.server(), delay.toMillis(), java.time.temporal.ChronoUnit.MILLIS));
            context.sendMessage("command.world.setborder.setdelay",
                    wp.key().asString(),
                    String.valueOf(x),
                    String.valueOf(z),
                    String.valueOf(dia),
                    String.valueOf(delay));
        }

        final WorldBorder newBorder = border.build();
        wp.setBorder(newBorder);
        return context.successResult();
    }


}
