/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import org.spongepowered.api.world.explosion.ExplosionBlockInteractions;
import net.kyori.adventure.sound.Sound;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.concurrent.TimeUnit;

@Command(
        aliases = "rocket",
        basePermission = FunPermissions.BASE_ROCKET,
        commandDescriptionKey = "rocket",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FunPermissions.EXEMPT_COOLDOWN_ROCKET),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FunPermissions.EXEMPT_WARMUP_ROCKET),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FunPermissions.EXEMPT_COST_ROCKET)
        },
        associatedPermissions = FunPermissions.OTHERS_ROCKET
)
public class RocketCommand implements ICommandExecutor {

    private final Parameter.Value<Double> velocityParameter = Parameter.builder(Double.class)
            .addParser(VariableValueParameters.doubleRange().min(0.0).build())
            .key("velocity")
            .build();

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("h", "hard"),
                Flag.of("g"),
                Flag.of("s", "silent"),
                Flag.of("e", "explosion"),
                Flag.of(this.velocityParameter, "v", "velocity")
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            NucleusParameters.ONE_PLAYER
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Player target = context.getPlayerFromArgs();
        final boolean isSelf = context.is(target);
        if (!isSelf && !context.testPermission(FunPermissions.OTHERS_ROCKET)) {
            return context.errorResult("command.rocket.noothers");
        }

        double v = 2;
        if (context.hasAny(this.velocityParameter)) {
            v = context.requireOne(this.velocityParameter);
        } else if (context.hasFlag("g")) {
            v = 0.5;
        } else if (context.hasFlag("h")) {
            v = 4;
        }

        if (context.hasFlag("e")) {
            final Explosion ex = Explosion.builder()
                    .location(target.serverLocation())
                    .canCauseFire(false)
                    .location(target.serverLocation())
                    .blockInteraction(ExplosionBlockInteractions.KEEP.get())
                    .shouldPlaySmoke(true)
                    .shouldDamageEntities(false)
                    .radius((float) v * 2.0f)
                    .build();

            target.serverLocation().world().triggerExplosion(ex);
            Sponge.server().scheduler().submit(
                    Task.builder()
                            .plugin(context.getServiceCollection().pluginContainer())
                            .execute(() -> ex.world().playSound(
                                    // TODO: what are Volume and Pitch defaults?
                                    Sound.sound(
                                            SoundTypes.ENTITY_FIREWORK_ROCKET_LAUNCH.get().key(),
                                            Sound.Source.MASTER,
                                            2,
                                            1),
                                    target.location().position()))
                            .delay(500, TimeUnit.MILLISECONDS)
                            .build()
            );
        }

        final Vector3d velocity = new Vector3d(0, v, 0);
        target.offer(Keys.VELOCITY, velocity);
        if (!context.hasFlag("s")) {
            context.sendMessageTo(target, "command.rocket.self");
        }

        if (!isSelf) {
            context.sendMessage("command.rocket.other", target.name());
        }

        return context.successResult();
    }
}
