/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.modules.chat.ChatPermissions;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.message.PlayerChatEvent;

@Command(
        aliases = {"me", "action"},
        basePermission = ChatPermissions.BASE_ME,
        commandDescriptionKey = "me",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ChatPermissions.EXEMPT_COOLDOWN_ME),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ChatPermissions.EXEMPT_WARMUP_ME),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ChatPermissions.EXEMPT_COST_ME)
        }
)
@EssentialsEquivalent({"me", "action", "describe"})
public class MeCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private final IChatMessageFormatterService chatMessageFormatterService;
    private NucleusTextTemplate mePrefix = NucleusTextTemplateImpl.empty();

    @Inject
    public MeCommand(final INucleusServiceCollection serviceCollection) {
        this.chatMessageFormatterService = serviceCollection.chatMessageFormatter();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.MESSAGE
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer player = context.getIfPlayer();
        final ITextStyleService textStyleService = context.getServiceCollection().textStyleService();
        final String message = textStyleService.stripPermissionless(
                ChatPermissions.CHAT_COLOR,
                ChatPermissions.CHAT_STYLE,
                player,
                context.requireOne(NucleusParameters.MESSAGE));

        final Component header = this.mePrefix.getForObject(context.getCommandSourceRoot());
        final ITextStyleService.TextFormat t = textStyleService.getLastColourAndStyle(header, null);
        final Component originalMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        final Component messageToSend = Component.text().color(t.colour().orElse(null)).style(t.style()).append(originalMessage).build();

        // We create an event so that other plugins can provide transforms, such as Boop, and that we
        // can catch it in ignore and mutes, and so can other plugins.
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame();
                final NoExceptionAutoClosable c =
                        this.chatMessageFormatterService.setPlayerNucleusChannelTemporarily(Util.CONSOLE_FAKE_UUID, new MeChannel(header))) {
            frame.addContext(EventContexts.SHOULD_FORMAT_CHANNEL, false);
            frame.pushCause(player);

            player.simulateChat(messageToSend, frame.currentCause());
        }
        return context.successResult();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.mePrefix = serviceCollection.textTemplateFactory().createFromAmpersandString(serviceCollection.configProvider().getModuleConfig(ChatConfig.class).getMePrefix());
    }

    public static final class MeChannel implements IChatMessageFormatterService.Channel {

        private final Component header;

        private MeChannel(final Component header) {
            this.header = header;
        }

        @Override
        public boolean willFormat() {
            return true;
        }

        @Override
        public Component formatMessage(final Audience source, final Component body) {
            return Component.text().append(this.header).append(body).build();
        }

    }
}
