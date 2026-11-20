/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world;

import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.modules.world.commands.CloneWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.CreateWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.DeleteWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.GameruleCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.InfoWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.ListWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.LoadWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.RenameWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.SetDifficultyWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.SetGamemodeWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.SetGameruleCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.SetSpawnWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.TeleportWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.UnloadWorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldSpawnCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.border.BorderCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.border.ResetBorderCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.border.SetBorderCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.properties.SetHardcoreCommand;
import io.github.nucleuspowered.nucleus.modules.world.commands.properties.SetLoadOnStartup;
import io.github.nucleuspowered.nucleus.modules.world.commands.properties.SetPvpEnabled;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.modules.world.listeners.EnforceGamemodeListener;
import io.github.nucleuspowered.nucleus.modules.world.listeners.LoginWorldLoadListener;
import io.github.nucleuspowered.nucleus.modules.world.listeners.WorldListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class WorldModule implements IModule.Configurable<WorldConfig> {

    public static final String ID = "world";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                BorderCommand.class,
                ResetBorderCommand.class,
                SetBorderCommand.class,
                SetHardcoreCommand.class,
                SetLoadOnStartup.class,
                SetPvpEnabled.class,
                CloneWorldCommand.class,
                CreateWorldCommand.class,
                DeleteWorldCommand.class,
                GameruleCommand.class,
                InfoWorldCommand.class,
                ListWorldCommand.class,
                LoadWorldCommand.class,
                RenameWorldCommand.class,
                SetDifficultyWorldCommand.class,
                SetGamemodeWorldCommand.class,
                SetGameruleCommand.class,
                SetSpawnWorldCommand.class,
                TeleportWorldCommand.class,
                UnloadWorldCommand.class,
                WorldCommand.class,
                WorldSpawnCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(WorldPermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Arrays.asList(
                EnforceGamemodeListener.class,
                LoginWorldLoadListener.class,
                WorldListener.class
        );
    }

    @Override
    public Class<WorldConfig> getConfigClass() {
        return WorldConfig.class;
    }
}
