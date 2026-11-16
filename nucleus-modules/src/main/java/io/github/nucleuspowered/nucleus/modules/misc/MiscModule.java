/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc;

import io.github.nucleuspowered.nucleus.modules.misc.commands.SpeedCommand;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.misc.commands.BlockInfoCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.EntityInfoCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.ExtinguishCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.FeedCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.HealCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.ItemInfoCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.PingCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.ServerStatCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.ServerTimeCommand;
import io.github.nucleuspowered.nucleus.modules.misc.commands.SuicideCommand;
import io.github.nucleuspowered.nucleus.modules.misc.config.MiscConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class MiscModule implements IModule.Configurable<MiscConfig> {

    public final static String ID = "misc";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                BlockInfoCommand.class,
                EntityInfoCommand.class,
                ExtinguishCommand.class,
                FeedCommand.class,
                HealCommand.class,
                ItemInfoCommand.class,
                PingCommand.class,
                ServerStatCommand.class,
                ServerTimeCommand.class,
                SpeedCommand.class,
                SuicideCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(MiscPermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.emptyList();
    }

    @Override
    public Class<MiscConfig> getConfigClass() {
        return MiscConfig.class;
    }

}
