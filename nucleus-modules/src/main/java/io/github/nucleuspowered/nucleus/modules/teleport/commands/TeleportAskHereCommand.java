/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.modules.teleport.events.RequestEvent;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.NotifyIfAFK;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.ICooldownService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

@EssentialsEquivalent("tpahere")
@NotifyIfAFK(NucleusParameters.Keys.PLAYER)
@Command(
        aliases = {"tpahere", "tpaskhere", "teleportaskhere"},
        basePermission = TeleportPermissions.BASE_TPAHERE,
        commandDescriptionKey = "tpahere",
        modifiers = {
                @CommandModifier(
                        value = CommandModifiers.HAS_WARMUP,
                        exemptPermission = TeleportPermissions.EXEMPT_WARMUP_TPAHERE,
                        onExecute = false,
                        onCompletion = false
                ),
                @CommandModifier(
                        value = CommandModifiers.HAS_COOLDOWN,
                        exemptPermission = TeleportPermissions.EXEMPT_COOLDOWN_TPAHERE
                ),
                @CommandModifier(
                        value = CommandModifiers.HAS_COST,
                        exemptPermission = TeleportPermissions.EXEMPT_COST_TPAHERE
                )
        },
        associatedPermissions = {
                TeleportPermissions.TPTOGGLE_EXEMPT,
                TeleportPermissions.TELEPORT_HERE_FORCE
        }
)
public class TeleportAskHereCommand implements ICommandExecutor {

    @Override
    public CommandElement[] parameters(final INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags()
                    .permissionFlag(TeleportPermissions.TELEPORT_HERE_FORCE, "f")
                    .buildWith(NucleusParameters.ONE_PLAYER.get(serviceCollection))
        };
    }

    @Override public Optional<ICommandResult> preExecute(final ICommandContext context) throws CommandException {
        final boolean cont = context.getServiceCollection()
                .getServiceUnchecked(PlayerTeleporterService.class)
                .canTeleportTo(context.getIfPlayer(), context.requireOne(NucleusParameters.Keys.PLAYER, Player.class));
        return cont ? Optional.empty() : Optional.of(context.failResult());
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Player target = context.requireOne(NucleusParameters.Keys.PLAYER, Player.class);
        if (context.is(target)) {
            return context.errorResult("command.teleport.self");
        }

        final Player src = context.getCommandSourceRoot();
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(src);
            // Before we do all this, check the event.
            final RequestEvent.PlayerToCause event = new RequestEvent.PlayerToCause(frame.getCurrentCause(), target);
            if (Sponge.getEventManager().post(event)) {
                return event.getCancelMessage()
                        .map(context::errorResultLiteral)
                        .orElseGet(() -> context.errorResult("command.tpa.eventfailed"));
            }

            final ICooldownService cooldownService = context.getServiceCollection().cooldownService();
            final Duration cooldownSeconds = Duration.ofSeconds(context.getCooldown());
            final String key = context.getCommandKey();
            final Consumer<Player> cooldownSetter = player -> cooldownService.setCooldown(
                    key,
                    player,
                    cooldownSeconds
            );
            context.getServiceCollection()
                    .getServiceUnchecked(PlayerTeleporterService.class)
                    .requestTeleport(
                        src,
                        target,
                        context.getCost(),
                        context.getWarmup(),
                        target,
                        src,
                        !context.getOne("f", Boolean.class).orElse(false),
                        false,
                        false,
                        cooldownSetter,
                        "command.tpahere.question"
            );

            return context.successResult();
        }
    }
}
