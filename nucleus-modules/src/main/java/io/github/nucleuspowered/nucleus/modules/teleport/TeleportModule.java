/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.commandspy.CommandSpyKeys;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAcceptCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAllHereCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAskAllHereCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAskCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportAskHereCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportDenyCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportHereCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportPositionCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.commands.TeleportToggleCommand;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfig;
import io.github.nucleuspowered.nucleus.modules.teleport.runnables.TeleportAsyncTask;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class TeleportModule implements IModule.Configurable<TeleportConfig> {

    public static final String ID = "teleport";
    private final IUserPreferenceService userPreferenceService;

    @Inject
    public TeleportModule(final IUserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @Override public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.registerService(PlayerTeleporterService.class, new PlayerTeleporterService(serviceCollection), false);
    }

    @Override public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                TeleportAcceptCommand.class,
                TeleportAllHereCommand.class,
                TeleportAskAllHereCommand.class,
                TeleportAskCommand.class,
                TeleportAskHereCommand.class,
                TeleportCommand.class,
                TeleportDenyCommand.class,
                TeleportHereCommand.class,
                TeleportPositionCommand.class,
                TeleportToggleCommand.class
        );
    }

    @Override public Optional<Class<?>> getPermissions() {
        return Optional.of(TeleportPermissions.class);
    }

    @Override public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.emptyList();
    }

    @Override public Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.singleton(TeleportAsyncTask.class);
    }

    @Override public Class<TeleportConfig> getConfigClass() {
        return TeleportConfig.class;
    }

    @Listener
    public void registerUserPreferenceKey(final RegisterRegistryValueEvent.GameScoped event) {
        event.registry(this.userPreferenceService.getRegistryResourceType(), step -> step.register(NucleusKeysProvider.TELEPORT_TARGETABLE_KEY,
                TeleportKeys.TELEPORT_TOGGLE));
    }

}
