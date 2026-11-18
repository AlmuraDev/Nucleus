/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

@EssentialsEquivalent(value = "world", notes = "The world command in Essentials was just a warp command.")
@Command(
        aliases = {"teleport", "tp"},
        basePermission = WorldPermissions.BASE_WORLD_TELEPORT,
        commandDescriptionKey = "world.teleport",
        parentCommand = WorldCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = WorldPermissions.EXEMPT_COOLDOWN_WORLD_TELEPORT),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = WorldPermissions.EXEMPT_WARMUP_WORLD_TELEPORT),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = WorldPermissions.EXEMPT_COST_WORLD_TELEPORT)
        },
        associatedPermissions = {
                WorldPermissions.WORLD_TELEPORT_OTHER,
                WorldPermissions.WORLDS_ACCESS_PERMISSION_PREFIX
        }
)
public class TeleportWorldCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            // Loaded or not: an unloaded world is loaded first, as Nucleus did on 1.12
            NucleusParameters.KNOWN_WORLD,
            serviceCollection.commandElementSupplier()
                .createOnlyOtherPlayerPermissionElement(WorldPermissions.WORLD_TELEPORT_OTHER)
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        // The player given, else the sender: getIfPlayer ignored the player argument, so nobody else could be sent
        final ServerPlayer player = context.getPlayerFromArgs();
        final ResourceKey worldKey = context.requireOne(NucleusParameters.KNOWN_WORLD);
        final Optional<ServerWorld> loaded = Sponge.server().worldManager().world(worldKey);
        if (loaded.isPresent()) {
            return this.teleport(context, player, loaded.get())
                    ? context.successResult()
                    : context.errorResult("command.world.teleport.failed", worldKey.asString());
        }

        context.sendMessage("command.world.load.start", worldKey.asString());
        Sponge.server().worldManager().loadWorld(worldKey).handle((world, exception) -> {
            context.getServiceCollection().schedulerService().runOnMainThread(() -> {
                // An empty result means the world couldn't be loaded
                if (exception != null || world == null || !world.isPresent()) {
                    context.sendMessage("command.world.teleport.failed", worldKey.asString());
                } else if (player.isOnline() && !this.teleport(context, player, world.get())) {
                    context.sendMessage("command.world.teleport.failed", worldKey.asString());
                }
            });
            return null;
        });
        return context.successResult();
    }

    private boolean teleport(final ICommandContext context, final ServerPlayer player, final ServerWorld world) {
        final Vector3d pos = world.properties().spawnPosition().toDouble();
        if (!player.transferToWorld(world, pos)) {
            return false;
        }

        final String name = world.key().asString();
        if (context.is(player)) {
            context.sendMessage("command.world.teleport.success", name);
        } else {
            context.sendMessage("command.world.teleport.successplayer",
                    context.getServiceCollection().playerDisplayNameService().getDisplayName(player), name);
            context.sendMessageTo(player, "command.world.teleport.success", name);
        }
        return true;
    }
}
