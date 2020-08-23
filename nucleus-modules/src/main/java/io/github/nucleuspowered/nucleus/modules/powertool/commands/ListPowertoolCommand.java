/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolPermissions;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Command(
        aliases = {"list", "ls"},
        basePermission = PowertoolPermissions.BASE_POWERTOOL,
        commandDescriptionKey = "powertool.list",
        parentCommand = PowertoolCommand.class
)
public class ListPowertoolCommand implements ICommandExecutor {

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final UUID uuid = context.getUniqueId().get();
        final boolean toggle = context.getServiceCollection().userPreferenceService()
                .getUnwrapped(uuid, NucleusKeysProvider.POWERTOOL_ENABLED);

        final PowertoolService service = context.getServiceCollection().getServiceUnchecked(PowertoolService.class);
        final Map<String, List<String>> powertools = service.getPowertools(uuid);

        if (powertools.isEmpty()) {
            return context.errorResult("command.powertool.list.none");
        }

        // Generate powertools.
        final List<Text> mesl = powertools.entrySet().stream().sorted((a, b) -> a.getKey()
                .compareToIgnoreCase(b.getKey()))
                .map(k -> from(service, context, k.getKey(), k.getValue())).collect(Collectors.toList());

        // Paginate the tools.
        Util.getPaginationBuilder(context.getCommandSourceRoot()).title(
                context.getMessage("command.powertool.list.header", toggle ? "&aenabled" : "&cdisabled"))
                .padding(Text.of(TextColors.YELLOW, "-")).contents(mesl)
                .sendTo(context.getCommandSourceRoot());

        return context.successResult();
    }

    private TextComponent from(
            final PowertoolService service,
            final ICommandContext context,
            final String powertool,
            final List<String> commands) {

        final Optional<ItemType> oit = Sponge.getRegistry().getType(ItemType.class, powertool);

        final Player src = context.getCommandSourceRoot();
        final UUID uuid = src.getUniqueId();

        // Create the click actions.
        final ClickAction viewAction = TextActions.executeCallback(pl -> Util.getPaginationBuilder(src)
                .title(context.getMessage("command.powertool.ind.header", powertool))
                .padding(Text.of(TextColors.GREEN, "-"))
                .contents(commands.stream().map(x -> Text.of(TextColors.YELLOW, x)).collect(Collectors.toList())).sendTo(src));

        final ClickAction deleteAction = TextActions.executeCallback(pl -> {
            service.clearPowertool(uuid, powertool);
            context.sendMessage("command.powertool.removed", powertool);
        });

        final TextColor tc = oit.map(itemType -> TextColors.YELLOW).orElse(TextColors.GRAY);

        // id - [View] - [Delete]
        return Text.builder().append(Text.of(tc, powertool)).append(Text.of(" - "))
                .append(context.getMessage("standard.view").toBuilder().color(TextColors.YELLOW).onClick(viewAction).build())
                .append(Text.of(" - "))
                .append(context.getMessage("standard.delete").toBuilder().color(TextColors.DARK_RED).onClick(deleteAction).build())
                .build();
    }
}
