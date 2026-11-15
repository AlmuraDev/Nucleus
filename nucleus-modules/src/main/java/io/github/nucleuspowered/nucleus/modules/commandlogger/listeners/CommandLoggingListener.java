/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandlogger.listeners;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.Platform;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.commandlogger.config.CommandLoggerConfig;
import io.github.nucleuspowered.nucleus.modules.commandlogger.services.CommandLoggerHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.util.CommandNameCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.block.entity.CommandBlock;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.minecart.CommandBlockMinecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.util.Nameable;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.Name;

public class CommandLoggingListener implements IReloadableService.Reloadable, ListenerBase {

    private final CommandLoggerHandler handler;
    private final IMessageProviderService messageProvider;
    private final IPlayerDisplayNameService displayNameProvider;
    private final Logger logger;
    private CommandLoggerConfig c;
    private Set<String> commandsToFilter = new HashSet<>();

    @Inject
    public CommandLoggingListener(final INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(CommandLoggerHandler.class);
        this.c = serviceCollection.configProvider().getModuleConfig(CommandLoggerConfig.class);
        this.messageProvider = serviceCollection.messageProvider();
        this.logger = serviceCollection.logger();
        this.displayNameProvider = serviceCollection.playerDisplayNameService();
    }

    @Listener(order = Order.LAST)
    public void onCommand(final ExecuteCommandEvent.Pre event) {
        final Object source = event.cause().root();
        // Check source.
        final boolean accept;
        if (source instanceof Player) {
            accept = this.c.getLoggerTarget().isLogPlayer();
        } else if (source instanceof CommandBlock || source instanceof CommandBlockMinecart) {
            accept = this.c.getLoggerTarget().isLogCommandBlock();
        } else if (source instanceof SystemSubject) {
            accept = this.c.getLoggerTarget().isLogConsole();
        } else {
            accept = this.c.getLoggerTarget().isLogOther();
        }
        if (!accept) {
            // We're not logging this!
            return;
        }
        final String name = PlainComponentSerializer.plain().serialize(this.displayNameProvider.getName(source, Component.text("unknown")));

        final String command = event.command().toLowerCase();
        final Set<String> commands = CommandNameCache.INSTANCE.getFromCommandAndSource(command, event.commandCause());
        commands.retainAll(this.commandsToFilter);

        // If whitelist, and we have the command, or if not blacklist, and we do not have the command.
        if (this.c.isWhitelist() == !commands.isEmpty()) {
            final String cause;
            if (this.c.isCauseEnhanced()) {
                final List<String> l = event.cause()
                        .all()
                        .stream()
                        .filter(x -> (x instanceof PluginContainer || x instanceof Nameable || x instanceof SystemSubject) && x != source)
                        // The game, API and implementation containers are in the cause of every command a player
                        // types, so they say nothing about who ran it
                        .filter(x -> !(x instanceof PluginContainer) || !CommandLoggingListener.isPlatformContainer((PluginContainer) x))
                        .map(x -> {
                            if (x instanceof Nameable) {
                                return ((Nameable) x).name();
                            } else if (x instanceof SystemSubject) {
                                return "Server";
                            } else {
                                final PluginContainer plugin = (PluginContainer) x;
                                return "(plugin) " + plugin.metadata().name().orElseGet(() -> plugin.metadata().id());
                            }
                        })
                        .collect(Collectors.toList());
                if (l.isEmpty()) {
                    cause = name;
                } else {
                    final List<String> stack = new ArrayList<>(l);
                    Collections.reverse(stack);
                    cause = String.format("[ %s -> ] %s", String.join(" -> ", stack), name);
                }
            } else {
                cause = name;
            }
            final String message = this.messageProvider.getMessageString("commandlog.message",
                    cause,
                    event.command(),
                    event.arguments());
            this.logger.info(message);
            this.handler.queueEntry(message);
        }
    }

    @Listener
    public void onShutdown(final StoppingEngineEvent<Server> event) {
        try {
            this.handler.onServerShutdown();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.c = serviceCollection.configProvider().getModuleConfig(CommandLoggerConfig.class);
        this.commandsToFilter = this.c.getCommandsToFilter().stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    private static boolean isPlatformContainer(final PluginContainer container) {
        final Platform platform = Sponge.platform();
        return container.equals(platform.container(Platform.Component.GAME))
                || container.equals(platform.container(Platform.Component.API))
                || container.equals(platform.container(Platform.Component.IMPLEMENTATION));
    }
}
