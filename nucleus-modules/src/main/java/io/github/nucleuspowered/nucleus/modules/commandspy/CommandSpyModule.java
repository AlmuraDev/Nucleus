/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.commandspy.commands.CommandSpyCommand;
import io.github.nucleuspowered.nucleus.modules.commandspy.config.CommandSpyConfig;
import io.github.nucleuspowered.nucleus.modules.commandspy.listeners.CommandSpyListener;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class CommandSpyModule implements IModule.Configurable<CommandSpyConfig> {

    public final static String ID = "command-spy";

    private final IUserPreferenceService userPreferenceService;

    @Inject
    public CommandSpyModule(final IUserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {

    }

    @Override public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Collections.singletonList(CommandSpyCommand.class);
    }

    @Override public Optional<Class<?>> getPermissions() {
        return Optional.of(CommandSpyPermissions.class);
    }

    @Override public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.singletonList(CommandSpyListener.class);
    }

    @Override public Class<CommandSpyConfig> getConfigClass() {
        return CommandSpyConfig.class;
    }

    @Listener
    public void registerUserPreferenceKey(final RegisterRegistryValueEvent.GameScoped event) {
        event.registry(this.userPreferenceService.getRegistryResourceType(), step -> step.register(NucleusKeysProvider.COMMAND_SPY_KEY, CommandSpyKeys.COMMAND_SPY));
    }
}
