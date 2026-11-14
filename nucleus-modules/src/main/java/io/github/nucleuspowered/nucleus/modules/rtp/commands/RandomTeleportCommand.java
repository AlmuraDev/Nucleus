/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernel;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITimingsService;
import io.github.nucleuspowered.nucleus.modules.rtp.RTPPermissions;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import io.github.nucleuspowered.nucleus.modules.rtp.events.RTPSelectedLocationEvent;
import io.github.nucleuspowered.nucleus.modules.rtp.options.RTPOptions;
import io.github.nucleuspowered.nucleus.modules.rtp.services.RTPService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.scaffold.task.CostCancellableTask;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@Command(
        aliases = {"rtp", "randomteleport", "rteleport"},
        basePermission = RTPPermissions.BASE_RTP,
        commandDescriptionKey = "rtp",
        modifiers = {
                @CommandModifier(
                        value = CommandModifiers.HAS_COOLDOWN,
                        exemptPermission = RTPPermissions.EXEMPT_COOLDOWN_RTP,
                        onCompletion = false
                ),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = RTPPermissions.EXEMPT_WARMUP_RTP),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = RTPPermissions.EXEMPT_COST_RTP)
        },
        associatedPermissions = {
                RTPPermissions.OTHERS_RTP,
                RTPPermissions.RTP_WORLDS
        }
)
public class RandomTeleportCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private RTPConfig rc = new RTPConfig();
    private final Map<ScheduledTask, UUID> cachedTasks = new WeakHashMap<>();

    private final ITimingsService.ITiming timings;

    @Inject
    public RandomTeleportCommand(final INucleusServiceCollection serviceCollection) {
        this.timings = serviceCollection.timingsService().of("RTP task");
    }

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherPlayerPermissionElement(RTPPermissions.OTHERS_RTP),
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer player = context.getPlayerFromArgs();
        synchronized (this.cachedTasks) {
            this.cachedTasks.keySet().removeIf(task -> !Sponge.server().scheduler().findTask(task.uniqueId()).isPresent());
            if (this.cachedTasks.containsValue(player.uniqueId())) {
                return context.errorResult("command.rtp.inprogress", player.name());
            }
        }

        // Get the current world.
        final ServerWorld wp;
        if (this.rc.getDefaultWorld().isPresent()) {
            wp = context.getOne(NucleusParameters.ONLINE_WORLD_OPTIONAL).orElseGet(() -> this.rc.getDefaultWorld().get());
        } else {
            wp = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL.key()).get();
        }

        if (this.rc.isPerWorldPermissions()) {
            final String name = wp.key().asString();
            if (!context.testPermission(RTPPermissions.RTP_WORLDS + "." + Util.worldPermissionSuffix(wp.key()))) {
                return context.errorResult("command.rtp.worldnoperm", name);
            }
        }

        context.sendMessage("command.rtp.searching");

        final RTPOptions options = new RTPOptions(this.rc, wp.key().asString());
        final RTPTask rtask = new RTPTask(
                context.getServiceCollection().pluginContainer(),
                wp,
                context,
                player.uniqueId(),
                this.rc.getNoOfAttempts(),
                options,
                context.getServiceCollection().getServiceUnchecked(RTPService.class).getKernel(wp),
                context.is(player) ? context.getCost() : 0);
        final Task task = Task.builder().execute(rtask).plugin(context.getServiceCollection().pluginContainer()).build();
        this.cachedTasks.put(Sponge.server().scheduler().submit(task), player.uniqueId());

        return context.successResult();
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.rc = serviceCollection.configProvider().getModuleConfig(RTPConfig.class);
    }

    /*
     * (non-Javadoc)
     *
     * The RTPTask class encapsulates the logic for the /rtp. Because TeleportHelper#getSafeLocation(Location) can be slow, particularly if there is a
     * large area to check, we opt for smaller areas, but to try multiple times. We separate each check by a couple of ticks so that the server
     * still gets to keep ticking, avoiding timeouts and too much lag.
     */
    private final class RTPTask extends CostCancellableTask {

        private final PluginContainer pluginContainer;
        private final Cause cause;
        private final ServerWorld targetWorld;
        private final ICommandContext source;
        private final UUID target;
        private final boolean isSelf;
        private final Logger logger;
        private int count;
        private final int maxCount;
        private final NucleusRTPService.RTPOptions options;
        private final RTPKernel kernel;

        private RTPTask(
                final PluginContainer pluginContainer,
                final ServerWorld target,
                final ICommandContext source,
                final UUID target1,
                final int maxCount,
                final NucleusRTPService.RTPOptions options,
                final RTPKernel kernel,
                final double cost) {
            super(source.getServiceCollection(), target1, cost);
            this.logger = source.getServiceCollection().logger();
            this.pluginContainer = pluginContainer;
            this.cause = Sponge.server().causeStackManager().currentCause();
            this.targetWorld = target;
            this.source = source;
            this.target = target1;
            this.isSelf = source.getAsPlayer().filter(x -> x.uniqueId().equals(target1)).isPresent();
            this.maxCount = maxCount;
            this.count = maxCount;
            this.options = options;
            this.kernel = kernel;
        }

        @Override public void accept(final ScheduledTask task) {
            this.count--;
            final ServerPlayer serverPlayer = Sponge.server().player(this.target).orElse(null);
            if (serverPlayer == null) {
                this.onCancel();
                return;
            }

            try (final ITimingsService.ITiming dummy = RandomTeleportCommand.this.timings.start()) {
                this.logger.debug(String.format("RTP of %s, attempt %s of %s", serverPlayer.name(), this.maxCount - this.count, this.maxCount));

                int counter = 0;
                while (++counter <= 10) {
                    try {
                        final Optional<ServerLocation> optionalLocation =
                                this.kernel.getLocation(serverPlayer.serverLocation(), this.targetWorld, this.options);
                        if (optionalLocation.isPresent()) {
                            final ServerLocation targetLocation = optionalLocation.get();
                            if (Sponge.eventManager().post(new RTPSelectedLocationEvent(
                                    targetLocation,
                                    serverPlayer,
                                    this.cause
                            ))) {
                                continue;
                            }

                            this.source.getServiceCollection().logger().debug(String.format("RTP of %s, found location %s, %s, %s",
                                    serverPlayer.name(),
                                    targetLocation.blockX(),
                                    targetLocation.blockY(),
                                    targetLocation.blockZ()));
                            if (serverPlayer.setLocation(targetLocation)) {
                                if (!this.isSelf) {
                                    this.source.sendMessageTo(serverPlayer, "command.rtp.other");
                                    this.source.sendMessage("command.rtp.successother",
                                            serverPlayer.name(),
                                            targetLocation.blockX(),
                                            targetLocation.blockY(),
                                            targetLocation.blockZ());
                                }

                                this.source.sendMessageTo(serverPlayer, "command.rtp.success",
                                        targetLocation.blockX(),
                                        targetLocation.blockY(),
                                        targetLocation.blockZ());
                                if (this.isSelf) {
                                    this.source.getServiceCollection()
                                            .cooldownService()
                                            .setCooldown(
                                                    this.source.getCommandKey(),
                                                    serverPlayer,
                                                    Duration.ofSeconds(this.source.getServiceCollection()
                                                            .commandMetadataService()
                                                            .getControl(RandomTeleportCommand.class)
                                                            .orElseThrow(IllegalStateException::new)
                                                            .getCooldown(serverPlayer))
                                            );
                                    synchronized (RandomTeleportCommand.this.cachedTasks) {
                                        RandomTeleportCommand.this.cachedTasks.remove(task);
                                    }
                                }
                            } else {
                                this.source.sendMessage("command.rtp.cancelled");
                                this.onCancel();
                            }
                            return;
                        }
                    } catch (final PositionOutOfBoundsException ignore) {
                        // treat as fail.
                    }
                }

                this.onUnsuccesfulAttempt(task, serverPlayer);
            }
        }

        private void onUnsuccesfulAttempt(final ScheduledTask task, final ServerPlayer serverPlayer) {
            synchronized (RandomTeleportCommand.this.cachedTasks) {
                if (this.count <= 0) {
                    this.source.getServiceCollection().logger()
                            .debug(String.format("RTP of %s was unsuccessful", serverPlayer.name()));
                    this.source.sendMessage("command.rtp.error");
                    this.onCancel();
                } else {
                    // We're using a scheduler to allow some ticks to go by between attempts to find a
                    // safe place.
                    final Task t = Task.builder().delay(Ticks.of(2)).execute(this).plugin(this.pluginContainer).build();
                    RandomTeleportCommand.this.cachedTasks.put(
                            Sponge.server().scheduler().submit(t),
                            serverPlayer.uniqueId()
                    );
                }

                RandomTeleportCommand.this.cachedTasks.remove(task);
            }
        }
    }

}
