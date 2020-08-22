/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands.gamemode;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
@EssentialsEquivalent({"gma", "adventure"})
@Command(
        aliases = {"gma", "adventure"},
        basePermission = {AdminPermissions.BASE_GAMEMODE, AdminPermissions.GAMEMODE_MODES_ADVENTURE},
        commandDescriptionKey = "gma",
        modifiers =
                {
                        @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_GAMEMODE,
                                useFrom = GamemodeCommand.class),
                        @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_GAMEMODE,
                                useFrom = GamemodeCommand.class),
                        @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_GAMEMODE,
                                useFrom = GamemodeCommand.class)
                }
)
public class AdventureGamemodeCommand extends GamemodeBase<Player> {

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> src) throws CommandException {
        return baseCommand(src, src.getIfPlayer(), GameModes.ADVENTURE);
    }
}
