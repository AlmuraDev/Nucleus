/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import org.spongepowered.api.world.explosion.ExplosionBlockInteractions;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.modules.fun.FunPermissions;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.standard.ResourceKeyedValueParameters;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.CatType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.vector.Vector3d;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Command(
        aliases = {"kittycannon", "kc"},
        basePermission = FunPermissions.BASE_KITTYCANNON,
        commandDescriptionKey = "kittycannon",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FunPermissions.EXEMPT_COOLDOWN_KITTYCANNON),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FunPermissions.EXEMPT_WARMUP_KITTYCANNON),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FunPermissions.EXEMPT_COST_KITTYCANNON)
        },
        associatedPermissions = FunPermissions.OTHERS_KITTYCANNON
)
public class KittyCannonCommand implements ICommandExecutor {

    private final Random random = new Random();
    private final Parameter.Value<Collection<ServerPlayer>> players;

    @Inject
    public KittyCannonCommand(final INucleusServiceCollection serviceCollection) {
        this.players = Parameter.builder(new TypeToken<Collection<ServerPlayer>>() {})
            .addParser(ResourceKeyedValueParameters.MANY_PLAYERS)
            .key("players")
            .optional()
            .requirements(cause -> serviceCollection.permissionService().hasPermission(cause, FunPermissions.OTHERS_KITTYCANNON))
            .build();
    }

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.builder().setPermission(FunPermissions.KITTYCANNON_DAMAGE).alias("d").alias("damageentities").build(),
                Flag.builder().setPermission(FunPermissions.KITTYCANNON_BREAK).alias("b").alias("breakblocks").build(),
                Flag.builder().setPermission(FunPermissions.KITTYCANNON_FIRE).alias("f").alias("fire").build()
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.players
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        Collection<ServerPlayer> playerList = context.requireOne(this.players);
        if (playerList.isEmpty()) {
            playerList = Collections.singletonList(context.getIfPlayer());
        }

        // For each subject, create a kitten, throw it out in the direction of the subject, and make it explode after between 2 and 5 seconds
        for (final ServerPlayer x : playerList) {
            this.getACat(context, x, context.hasFlag("d"), context.hasFlag("b"), context.hasFlag("f"));
        }
        return context.successResult();
    }

    private void getACat(final ICommandContext context, final ServerPlayer spawnAt, final boolean damageEntities, final boolean breakBlocks,
            final boolean causeFire) {
        // Fire it in the direction that the subject is facing with a speed of 0.5 to 3.5, plus the subject's current velocity.
        final Vector3d headRotation = spawnAt.headDirection();
        final Quaterniond rot = Quaterniond.fromAxesAnglesDeg(headRotation.x(), -headRotation.y(), headRotation.z());
        final Vector3d velocity = spawnAt.get(Keys.VELOCITY).orElse(Vector3d.ZERO).add(rot.rotate(Vector3d.UNIT_Z).mul(5 * this.random.nextDouble() + 1));
        final ServerWorld world = spawnAt.world();
        final List<CatType> catTypes = RegistryTypes.CAT_TYPE.get().stream().collect(Collectors.toList());
        final Entity cat = world.createEntity(
                EntityTypes.CAT.get(),
                spawnAt.position().add(0, 1, 0).add(spawnAt.direction()));
        cat.offer(Keys.CAT_TYPE, catTypes.get(this.random.nextInt(catTypes.size())));

        Sponge.server().scheduler().submit(
                Task.builder().interval(Ticks.of(5))
                        .delay(Ticks.of(5))
                        .execute(new CatTimer(world.key(),
                                cat.uniqueId(),
                                spawnAt,
                                this.random.nextInt(60) + 20,
                                damageEntities,
                                breakBlocks,
                                causeFire))
                        .plugin(context.getServiceCollection().pluginContainer())
                        .build());

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSourceRoot());
            world.spawnEntity(cat);
        }

        cat.offer(Keys.VELOCITY, velocity);
    }

    private static final class CatTimer implements Consumer<ScheduledTask> {

        private final UUID entity;
        private final ResourceKey world;
        private final Player player;
        private final boolean damageEntities;
        private final boolean causeFire;
        private final boolean breakBlocks;
        private int ticksToDestruction;

        private CatTimer(
                final ResourceKey world, final UUID entity, final Player player, final int ticksToDestruction, final boolean damageEntities,
                final boolean breakBlocks, final boolean causeFire) {
            this.entity = entity;
            this.ticksToDestruction = ticksToDestruction;
            this.world = world;
            this.player = player;
            this.damageEntities = damageEntities;
            this.breakBlocks = breakBlocks;
            this.causeFire = causeFire;
        }

        @Override
        public void accept(final ScheduledTask task) {
            final Optional<ServerWorld> oWorld = Sponge.server().worldManager().world(this.world);
            if (!oWorld.isPresent()) {
                task.cancel();
                return;
            }

            final Optional<Entity> oe = oWorld.get().entity(this.entity);
            if (!oe.isPresent()) {
                task.cancel();
                return;
            }

            final Entity e = oe.get();
            if (e.isRemoved()) {
                task.cancel();
                return;
            }

            this.ticksToDestruction -= 5;
            if (this.ticksToDestruction <= 0 || e.onGround().get()) {
                // Cat explodes.
                final Explosion explosion = Explosion.builder().location(e.serverLocation()).canCauseFire(this.causeFire)
                    .shouldDamageEntities(this.damageEntities).shouldPlaySmoke(true).blockInteraction(this.breakBlocks ? ExplosionBlockInteractions.DESTROY.get() : ExplosionBlockInteractions.KEEP.get())
                    .radius(2).build();
                e.remove();
                try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                    frame.pushCause(this.player);
                    oWorld.get().triggerExplosion(explosion);
                }

                task.cancel();
            }
        }
    }
}
