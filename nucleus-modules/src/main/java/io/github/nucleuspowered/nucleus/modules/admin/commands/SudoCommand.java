/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.core.configurate.config.CommonPermissionLevelConfig;
import io.github.nucleuspowered.nucleus.modules.admin.AdminModule;
import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.modules.admin.config.AdminConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;

@Command(aliases = {"sudo"},
        basePermission = AdminPermissions.BASE_SUDO,
        commandDescriptionKey = "sudo",
        associatedPermissionLevelKeys = AdminModule.SUDO_LEVEL_KEY)
@EssentialsEquivalent("sudo")
public class SudoCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private CommonPermissionLevelConfig levelConfig = new CommonPermissionLevelConfig();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[]{
                NucleusParameters.ONE_PLAYER,
                NucleusParameters.COMMAND
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer pl = context.requireOne(NucleusParameters.ONE_PLAYER);
        final String cmd = context.requireOne(NucleusParameters.COMMAND);
        if (context.is(pl) || (!context.isConsoleAndBypass() && context.testPermissionFor(pl, AdminPermissions.SUDO_EXEMPT))) {
            return context.errorResult("command.sudo.noperms");
        }

        if (this.levelConfig.isUseLevels() &&
                !context.isPermissionLevelOkay(pl,
                        AdminModule.SUDO_LEVEL_KEY,
                        AdminPermissions.BASE_SUDO,
                        this.levelConfig.isCanAffectSameLevel())) {
            // Failure.
            return context.errorResult("command.modifiers.level.insufficient", pl.name());
        }

        if (cmd.startsWith("c:")) {
            if (cmd.equals("c:")) {
                return context.errorResult("command.sudo.chatfail");
            }

            final Component rawMessage = Component.text(cmd.split(":", 2)[1]);
            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                frame.pushCause(context.getCommandSourceRoot());
                frame.pushCause(pl); // on top
                frame.addContext(EventContextKeys.SIMULATED_PLAYER, pl.profile());

                pl.simulateChat(rawMessage, frame.currentCause());
            }

            return context.successResult();
        }

        context.sendMessage("command.sudo.force", pl.name(), cmd);
        Sponge.server().commandManager().process(pl, cmd);
        return context.successResult();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.levelConfig = serviceCollection.configProvider().getModuleConfig(AdminConfig.class).getLevelConfig();
    }
}
