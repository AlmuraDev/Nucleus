/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import org.spongepowered.api.effect.VanishState;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.module.afk.NucleusAFKService;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.ListConfig;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.PlayerInfoConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.pagination.PaginationList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EssentialsEquivalent({"list", "who", "playerlist", "online", "plist"})
@Command(
        aliases = {"list", "listplayers", "ls"},
        basePermission = PlayerInfoPermissions.BASE_LIST,
        commandDescriptionKey = "list",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = PlayerInfoPermissions.EXEMPT_COOLDOWN_LIST),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = PlayerInfoPermissions.EXEMPT_WARMUP_LIST),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = PlayerInfoPermissions.EXEMPT_COST_LIST)
        },
        associatedPermissions = PlayerInfoPermissions.LIST_SEEVANISHED
)
public class ListPlayerCommand implements ICommandExecutor, IReloadableService.Reloadable {

    public static final String LIST_OPTION = "nucleus.list.group";

    private ListConfig listConfig = new ListConfig();

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final boolean showVanished = context.testPermission(PlayerInfoPermissions.LIST_SEEVANISHED);

        final Collection<ServerPlayer> players = Sponge.server().onlinePlayers();
        final long playerCount = players.size();
        final long hiddenCount = players.stream().filter(x -> x.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false)).count();

        final Component header;
        if (showVanished && hiddenCount > 0) {
            header = context.getMessage("command.list.playercount.hidden", String.valueOf(playerCount),
                    String.valueOf(Sponge.server().maxPlayers()), String.valueOf(hiddenCount));
        } else {
            header = context.getMessage("command.list.playercount.base", String.valueOf(playerCount - hiddenCount),
                    String.valueOf(Sponge.server().maxPlayers()));
        }

        final PaginationList.Builder builder = Util.getPaginationBuilder(context.audience()).title(header);

        if (this.listConfig.isGroupByPermissionGroup()) {
            builder.contents(this.listByPermissionGroup(context, showVanished));
        } else {
            // If we have players, send them on.
            builder.contents(this.getPlayerList(players, showVanished, context));
        }

        builder.sendTo(context.audience());
        return context.successResult();
    }

    private List<Component> listByPermissionGroup(final ICommandContext context, final boolean showVanished) {
        // Messages
        final List<Component> messages = new ArrayList<>();

        final String defName = this.listConfig.getDefaultGroupName();
        final Map<String, List<ServerPlayer>> groupToPlayer = this.playerList(
                context,
                showVanished,
                defName
        );

        this.listConfig.getOrder().forEach(alias -> {
            final List<ServerPlayer> plList = groupToPlayer.get(alias);
            if (plList != null && !plList.isEmpty()) {
                // Get and put the player list into the map, if there is a
                // player to show. There might not be, they might be vanished!
                this.getList(plList, showVanished, messages, alias, context);
            }

            groupToPlayer.remove(alias);
        });

        groupToPlayer.entrySet().stream()
                .filter(x -> !x.getValue().isEmpty())
                .filter(x -> !x.getKey().equals(defName))
                .sorted((x, y) -> x.getKey().compareToIgnoreCase(y.getKey()))
                .forEach(x -> this.getList(x.getValue(), showVanished, messages, x.getKey(), context));

        final List<ServerPlayer> pl = groupToPlayer.get(defName);
        if (pl != null && !pl.isEmpty()) {
            this.getList(pl, showVanished, messages, defName, context);
        }

        return messages;
    }

    private Map<String, List<ServerPlayer>> playerList(
            final ICommandContext context,
            final boolean showVanished,
            final String def) {
        final IPlayerOnlineService playerOnlineService = context.getServiceCollection().playerOnlineService();
        final IPermissionService permissionService = context.getServiceCollection().permissionService();
        final Map<String, List<ServerPlayer>> map = new HashMap<>();
        for (final ServerPlayer player : Sponge.server().onlinePlayers()) {
            if (showVanished || context.getAsPlayer().map(x -> playerOnlineService.isOnline(x, player.user())).orElse(true)) {
                String perm = permissionService.getOptionFromSubject(player, LIST_OPTION).orElse(def);
                if (perm.trim().isEmpty()) {
                    perm = def;
                }
                map.computeIfAbsent(perm, y -> new ArrayList<>()).add(player);
            }
        }
        return map;
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.listConfig = serviceCollection.configProvider().getModuleConfig(PlayerInfoConfig.class).getList();
    }

    private void getList(final Collection<ServerPlayer> player, final boolean showVanished, final List<Component> messages, final String groupName,
            final ICommandContext context) {
        final Component groupNameTextComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(groupName);
        final List<Component> m = this.getPlayerList(player, showVanished, context);
        if (this.listConfig.isCompact()) {
            boolean isFirst = true;
            for (final Component y : m) {
                final TextComponent.Builder tb = Component.text();
                if (isFirst) {
                    tb.append(Component.text(": ", NamedTextColor.YELLOW));
                }
                isFirst = false;
                messages.add(tb.append(y).build());
            }
        } else {
            messages.add(LinearComponents.linear(groupNameTextComponent, Component.text(":", NamedTextColor.YELLOW)));
            messages.addAll(m);
        }
    }

    private List<Component> getPlayerList(final Collection<ServerPlayer> playersToList, final boolean showVanished, final ICommandContext context) {
        final NucleusTextTemplate template =
                context.getServiceCollection().textTemplateFactory().createFromAmpersandString(this.listConfig.getListTemplate());
        @Nullable final NucleusAFKService afkService = NucleusAPI.getAFKService().orElse(null);
        final Component afk = context.getMessage("command.list.afk");
        final Component hidden = context.getMessage("command.list.hidden");

        final List<Component> playerList = playersToList.stream().filter(x -> showVanished || !x.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false))
                .sorted((x, y) -> x.name().compareToIgnoreCase(y.name())).map(x -> {
                    final TextComponent.Builder tb = Component.text();
                    boolean appendSpace = false;
                    if (afkService != null && afkService.isAFK(x.uniqueId())) {
                        tb.append(afk);
                        appendSpace = true;
                    }

                    if (x.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false)) {
                        tb.append(hidden);
                        appendSpace = true;
                    }

                    if (appendSpace) {
                        tb.append(Component.space());
                    }

                    if (template != null) { // it shouldn't be, but if it is, fallback...
                        return tb.append(template.getForObject(x)).build();
                    } else {
                        return tb.append(context.getDisplayName(x.uniqueId())).build();
                    }
                }).collect(Collectors.toList());

        if (this.listConfig.isCompact() && !playerList.isEmpty()) {
            final List<Component> toReturn = new ArrayList<>();
            final List<List<Component>> parts = new ArrayList<>();
            final int maxSize = this.listConfig.getMaxPlayersPerLine();
            List<Component> l = null;
            for (final Component part : playerList) {
                if (l == null || l.size() == maxSize) {
                    l = new ArrayList<>();
                    parts.add(l);
                }
                l.add(part);
            }
            //Lists.partition(playerList, this.listConfig.getMaxPlayersPerLine());
            for (final List<Component> p : parts) {
                final TextComponent.Builder tb = Component.text();
                boolean isFirst = true;
                for (final Component text : p) {
                    if (!isFirst) {
                        tb.append(Component.text(", ", NamedTextColor.WHITE));
                    }

                    tb.append(text);
                    isFirst = false;
                }

                toReturn.add(tb.build());
            }

            return toReturn;
        }

        return playerList;
    }
}
