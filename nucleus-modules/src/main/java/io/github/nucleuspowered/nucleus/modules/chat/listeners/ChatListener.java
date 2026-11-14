/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.listeners;

import io.github.nucleuspowered.nucleus.core.util.NucleusChatType;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.placeholder.NucleusPlaceholderService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import io.github.nucleuspowered.nucleus.core.util.AdventureUtils;
import io.github.nucleuspowered.nucleus.modules.chat.ChatPermissions;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.modules.chat.services.ChatService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.PlayerChatEvent;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A listener that modifies all chat messages. Uses the
 * {@link NucleusPlaceholderService}, which
 * should be used if tokens need to be registered.
 */
public class ChatListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final ChatService chatService;
    private final ITextStyleService textStyleService;
    private final IPermissionService permissionService;
    private final IChatMessageFormatterService chatMessageFormatterService;

    private ChatConfig chatConfig;

    @Inject
    public ChatListener(final INucleusServiceCollection serviceCollection) {
        this.chatService = serviceCollection.getServiceUnchecked(ChatService.class);
        this.textStyleService = serviceCollection.textStyleService();
        this.chatConfig = serviceCollection.configProvider().getModuleConfig(ChatConfig.class);
        this.permissionService = serviceCollection.permissionService();
        this.chatMessageFormatterService = serviceCollection.chatMessageFormatter();
    }

    // We do this first so that other plugins can alter it later if needs be.
    @Listener(order = Order.EARLY, beforeModifications = true)
    public void onPlayerChatEventEarly(final PlayerChatEvent.Submit event, @Root final ServerPlayer player) {
        if (!this.chatConfig.isTryForceFormatting()) {
            this.onPlayerChatEvent(event, player);
        }
    }

    @Listener(order = Order.LATE)
    public void onPlayerChatEventLate(final PlayerChatEvent.Submit event, @Root final ServerPlayer player) {
        if (this.chatConfig.isTryForceFormatting()) {
            this.onPlayerChatEvent(event, player);
        }
    }

    private void onPlayerChatEvent(final PlayerChatEvent.Submit event, @Root final ServerPlayer player) {
        if (this.chatMessageFormatterService.getNucleusChannel(player.uniqueId())
                .map(IChatMessageFormatterService.Channel::willFormat).orElse(false)) {
            return;
        }

        Component baseMessage;
        if (this.chatConfig.isIgnoreOtherPlugins()) {
            baseMessage = event.originalMessage();
        } else {
            baseMessage = event.message();
        }

        if (this.chatConfig.isTryRemoveMinecraftPrefix()) {
            final Pattern removal = Pattern.compile("<" + player.name() + ">");
            baseMessage = baseMessage.replaceText(function -> function.match(removal).replacement(Component.empty()));
        }

        final ChatService.TemplateCache ctc;
        if (this.chatConfig.isUseGroupTemplates()) {
            ctc = this.chatService.getTemplateNow(player);
        } else {
            ctc = this.chatService.getDefaultTemplate();
        }

        final TextComponent.Builder builder = Component.text();
        final Component header = ctc.getPrefix().getForObject(player);
        final Component footer = ctc.getSuffix().getForObject(player);
        if (!AdventureUtils.isEmpty(header)) {
            builder.append(header);
        }
        final Component body = this.chatConfig.isModifyMessage() ? this.useMessage(player, baseMessage, ctc) : baseMessage;
        if (!AdventureUtils.isEmpty(footer)) {
            builder.append(footer);
        }
        // nucleus:chat is just sender + content: the template's prefix already holds the name and separator
        if (!NucleusChatType.apply(event, header)) {
            event.setSender(header);
        }
        event.setMessage(AdventureUtils.isEmpty(footer) ? body : LinearComponents.linear(body, footer));
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(ChatConfig.class).isModifychat();
    }

    private TextComponent useMessage(final ServerPlayer player, final Component rawMessage, final ChatService.TemplateCache templateCache) {
        final ChatTemplateConfig chatTemplateConfig = templateCache.getConfig();
        String m = LegacyComponentSerializer.legacyAmpersand().serialize(rawMessage);
        if (this.chatConfig.isRemoveBlueUnderline()) {
            m = m.replaceAll("&9&n([A-Za-z0-9-.]+)(&r)?", "$1");
        }

        m = this.textStyleService.stripPermissionless(ChatPermissions.CHAT_COLOR, ChatPermissions.CHAT_STYLE, player, m);

        final Component result;
        if (this.permissionService.hasPermission(player, ChatPermissions.CHAT_URLS)) {
            result = this.textStyleService.addUrls(m, !this.chatConfig.isRemoveBlueUnderline());
        } else {
            result = LegacyComponentSerializer.legacyAmpersand().deserialize(m);
        }

        final String chatcol = this.permissionService.getOptionFromSubject(player, "chatcolour", "chatcolor").orElseGet(chatTemplateConfig::getChatcolour);
        final String chatstyle = this.permissionService.getOptionFromSubject(player, "chatstyle").orElseGet(chatTemplateConfig::getChatstyle);

        return Component.text()
                .color(this.textStyleService.getColourFromString(chatcol).orElse(null))
                .style(this.textStyleService.getTextStyleFromString(chatstyle))
                .append(result)
                .build();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.chatConfig = serviceCollection.configProvider().getModuleConfig(ChatConfig.class);
    }
}
