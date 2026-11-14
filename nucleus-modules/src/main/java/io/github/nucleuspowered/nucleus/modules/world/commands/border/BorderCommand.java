/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import org.spongepowered.api.Sponge;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.commands.WorldCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Command(
        aliases = { "border" },
        basePermission = WorldPermissions.BASE_WORLD_BORDER,
        commandDescriptionKey = "world.border",
        parentCommand = WorldCommand.class
)
public class BorderCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONLINE_WORLD_OPTIONAL
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerWorld wp = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.ONLINE_WORLD_OPTIONAL.key())
                .orElseThrow(() -> context.createException("command.world.player"));
        final List<Component> worldBorderInfo = new ArrayList<>();

        final WorldBorder worldBorder = wp.properties().worldBorder();
        final Vector2d centre = worldBorder.center();
        final int currentDiameter = (int) worldBorder.diameter();
        final int targetDiameter = (int) worldBorder.targetDiameter();

        // Border centre
        worldBorderInfo.add(context.getMessage("command.world.border.centre", String.valueOf(centre.floorX()), String.valueOf(centre.floorY())));
        worldBorderInfo.add(context.getMessage("command.world.border.currentdiameter", currentDiameter));

        if (currentDiameter != targetDiameter) {
            worldBorderInfo.add(context.getMessage("command.world.border.targetdiameter", targetDiameter,
                    String.valueOf(worldBorder.timeUntilTargetDiameter().expectedDuration(Sponge.server()).getSeconds())));
        }

        Util.getPaginationBuilder(context.audience())
                .contents(worldBorderInfo)
                .title(context.getMessage("command.world.border.title", wp.key().asString()))
                .padding(Component.text("=", NamedTextColor.GREEN))
                .sendTo(context.audience());
        return context.successResult();
    }
}
