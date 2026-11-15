/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfig;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.message.PlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Optional;

public class StaffChatMessageChannel implements IChatMessageFormatterService.Channel, IReloadableService.Reloadable {

    private static StaffChatMessageChannel INSTANCE = null;

    public static StaffChatMessageChannel getInstance() {
        if (StaffChatMessageChannel.INSTANCE == null) {
            throw new IllegalStateException("StaffChatMessageChannel#Instance");
        }

        return StaffChatMessageChannel.INSTANCE;
    }


    private final IPermissionService permissionService;
    private final IUserPreferenceService userPreferenceService;
    private NucleusTextTemplateImpl template = NucleusTextTemplateImpl.empty();
    private TextColor colour = NamedTextColor.BLUE;
    private boolean formatting = false;

    @Inject
    StaffChatMessageChannel(final INucleusServiceCollection serviceCollection) {
        serviceCollection.reloadableService().registerReloadable(this);
        this.permissionService = serviceCollection.permissionService();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        StaffChatMessageChannel.INSTANCE = this;
    }

    @Override
    public boolean willFormat() {
        return true;
    }

    public boolean formatMessages() {
        return this.formatting;
    }

    public void sendMessageFrom(final Audience source, final Component text) {
        final Component res = this.formatMessage(source, text);
        this.receivers().sendMessage(res, MessageType.CHAT);
    }

    @Override
    public Component formatMessage(final Audience source, final Component body) {
        final Component prefix = this.template.getForObject(source);
        if (this.colour != null) {
            return LinearComponents.linear(
                    prefix,
                    this.colour,
                    body
            );
        }
        return LinearComponents.linear(prefix, body);
    }

    @Override
    public Optional<Predicate<ServerPlayer>> recipientFilter() {
        return Optional.of(this::test);
    }

    @Override
    public ForwardingAudience receivers() {
        final List<Audience> audienceList = new ArrayList<>();
        Sponge.server().onlinePlayers().stream()
                .filter(this::test)
                .forEach(audienceList::add);
        audienceList.add(Sponge.systemSubject());
        return Audience.audience(audienceList);
    }

    @Override
    public boolean ignoreIgnoreList() {
        return true;
    }

    private boolean test(final ServerPlayer player) {
        if (this.permissionService.hasPermission(player, StaffChatPermissions.BASE_STAFFCHAT)) {
            return this.userPreferenceService
                    .getPreferenceFor(player.uniqueId(), StaffChatKeys.VIEW_STAFF_CHAT)
                    .orElse(true);
        }

        return false;
    }

    public void onReload(final INucleusServiceCollection serviceCollection) {
        final StaffChatConfig sc = serviceCollection.configProvider().getModuleConfig(StaffChatConfig.class);
        this.template = serviceCollection.textTemplateFactory().createFromAmpersandString(sc.getMessageTemplate());
        this.formatting = sc.isIncludeStandardChatFormatting();
        this.colour = serviceCollection.textStyleService().getColourFromString(sc.getMessageColour()).orElse(null);
    }

}
