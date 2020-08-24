/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.modules.chat.ChatPermissions;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Collection;
import java.util.Optional;

import com.google.inject.Inject;

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
    private ChatConfig config = new ChatConfig();

    @Inject
    public MeCommand(final INucleusServiceCollection serviceCollection) {
        this.chatMessageFormatterService = serviceCollection.chatMessageFormatter();
    }

    @Override
    public CommandElement[] parameters(final INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.MESSAGE
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ITextStyleService textStyleService = context.getServiceCollection().textStyleService();
        final String message = textStyleService.stripPermissionless(
                ChatPermissions.CHAT_COLOR,
                ChatPermissions.CHAT_STYLE,
                context.getCommandSourceRoot(),
                context.requireOne(NucleusParameters.Keys.MESSAGE, String.class));

        final TextComponent header = this.config.getMePrefix().getForObject(context.getCommandSourceRoot());
        final ITextStyleService.TextFormat t = textStyleService.getLastColourAndStyle(header, null);
        final TextComponent originalMessage = TextSerializers.FORMATTING_CODE.deserialize(message);
        final MessageEvent.MessageFormatter formatter = new MessageEvent.MessageFormatter(
                Text.builder().color(t.colour()).style(t.style())
                        .append(TextSerializers.FORMATTING_CODE.deserialize(message)).toText()
        );

        // Doing this here rather than in the constructor removes the < > notation.
        formatter.setHeader(Text.of());

        // We create an event so that other plugins can provide transforms, such as Boop, and that we
        // can catch it in ignore and mutes, and so can other plugins.
        final CommandSource src = context.getCommandSourceRoot();
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame();
                final NoExceptionAutoClosable c =
                        this.chatMessageFormatterService.setPlayerNucleusChannelTemporarily(Util.CONSOLE_FAKE_UUID, new MeChannel(header))) {
            frame.addContext(EventContexts.SHOULD_FORMAT_CHANNEL, false);
            if (frame.getCurrentCause().root() != src) {
                frame.pushCause(src);
            }

            final MessageChannelEvent.Chat event =
                    SpongeEventFactory.createMessageChannelEventChat(
                            frame.getCurrentCause(),
                            src.getMessageChannel(),
                            Optional.of(src.getMessageChannel()),
                            formatter,
                            originalMessage,
                            false);

            if (Sponge.getEventManager().post(event)) {
                return context.errorResult("command.me.cancel");
            }

            event.getChannel().orElse(MessageChannel.TO_ALL).send(src, Util.applyChatTemplate(event.getFormatter()), ChatTypes.CHAT);
        }
        return context.successResult();
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(ChatConfig.class);
    }

    public static class MeChannel implements IChatMessageFormatterService.Channel {

        private final TextComponent header;

        private MeChannel(final TextComponent header) {
            this.header = header;
        }

        @Override
        public boolean willFormat() {
            return true;
        }

        @Override
        public void formatMessageEvent(final CommandSource source, final MessageEvent.MessageFormatter formatters) {
            formatters.setHeader(Text.of(formatters.getHeader(), this.header));
        }

        @Override
        public Collection<MessageReceiver> receivers() {
            return MessageChannel.TO_ALL.getMembers();
        }

    }
}
