/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.services.UniqueUserService;
import io.github.nucleuspowered.nucleus.core.services.IInitService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.placeholder.standard.NamePlaceholder;
import io.github.nucleuspowered.nucleus.core.services.impl.placeholder.standard.NamedOptionPlaceholder;
import io.github.nucleuspowered.nucleus.core.services.impl.placeholder.standard.OptionPlaceholder;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlaceholderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.placeholder.PlaceholderComponent;
import org.spongepowered.api.placeholder.PlaceholderContext;
import org.spongepowered.api.placeholder.PlaceholderParser;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Nameable;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class PlaceholderService implements IPlaceholderService, IInitService {

    private static final Pattern SUFFIX_PATTERN = Pattern.compile(":([sp]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEPARATOR;
    private final PlaceholderParser optionParser;
    private final PlaceholderParser emptyParser;
    private final Map<String, PlaceholderMetadata> parsers = new HashMap<>();

    static {
        SEPARATOR = buildModifiers();
    }

    private static Pattern buildModifiers() {
        final StringBuilder builder = new StringBuilder(":([");
        for (final TextModifiers m : TextModifiers.values()) {
            builder.append(m.getKey());
        }
        builder.append("]+)$");
        return Pattern.compile(builder.toString());
    }

    @Inject
    public PlaceholderService(final INucleusServiceCollection serviceCollection) {
        this.optionParser = new OptionPlaceholder(serviceCollection.permissionService());
        this.emptyParser =
                PlaceholderParser.builder().parser(p -> Component.empty()).build();
    }

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        // player, variables, map?
        this.registerToken("empty", this.emptyParser, false);
        final NamePlaceholder<Nameable> normalName = new NamePlaceholder<>(
                Nameable.class,
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::addCommandToName,
                "nucleus:name"
        );
        this.registerToken("name", normalName);
        this.registerToken("playername", normalName, true, true);
        this.registerToken("subject", new NamePlaceholder<>(
                Nameable.class,
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::addCommandToName,
                "nucleus:subject",
                true));

        final NamePlaceholder<ServerPlayer> displayName = new NamePlaceholder<>(
                ServerPlayer.class,
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::getDisplayName,
                "nucleus:displayname"
        );
        this.registerToken("player", displayName);
        this.registerToken("playerdisplayname", displayName, true, true);
        this.registerToken("displayname", displayName, true, true);

        final IPermissionService permissionService = serviceCollection.permissionService();
        this.registerToken("option", this.optionParser);
        this.registerToken("prefix", new NamedOptionPlaceholder(permissionService, "prefix"));
        this.registerToken("suffix", new NamedOptionPlaceholder(permissionService, "suffix"));

        this.registerToken("maxplayers", PlaceholderParser.builder()
                .parser(p -> Component.text(Sponge.server().maxPlayers()))
                .build());
        this.registerToken("onlineplayers", PlaceholderParser.builder()
                        .parser(p -> Component.text(Sponge.server().onlinePlayers().size()))
                        .build());
        this.registerToken("currentworld", PlaceholderParser.builder()
                .parser(placeholder -> Component.text(PlaceholderService.getWorld(placeholder).key().formatted()))
                .build());
        this.registerToken("time",
                PlaceholderParser.builder()
                        .parser(placeholder ->
                                Component.text(
                                        Util.getTimeFromDayTime(serviceCollection.messageProvider(),
                                                PlaceholderService.getWorld(placeholder).dayTime())))
                        .build());

        this.registerToken("uniquevisitor",
                PlaceholderParser.builder()
                        .parser(placeholder -> Component.text(serviceCollection.getServiceUnchecked(UniqueUserService.class).getUniqueUserCount()))
                        .build());
        this.registerToken("ipaddress",
                PlaceholderParser.builder()
                        .parser(placeholder -> placeholder.associatedObject().filter(x -> x instanceof ServerPlayer)
                                .map(x -> Component.text(((ServerPlayer) x).connection().address().getAddress().toString()))
                                .orElse(Component.text("localhost")))
                        .build());
    }

    @Override
    public Set<PlaceholderParser> getParsers() {
        return this.parsers.values().stream().map(PlaceholderMetadata::getParser).collect(Collectors.toSet());
    }

    @Override
    public ComponentLike parse(@Nullable final Object commandSource, final String input) {
        String token = input.toLowerCase().trim().replace("{{", "").replace("}}", "");
        final Matcher m = SUFFIX_PATTERN.matcher(token);
        final List<Function<Component, Component>> modifiersCollection;
        if (m.find(0)) {
            final String match = m.group(1).toLowerCase();
            modifiersCollection = new ArrayList<>();
            for (final TextModifiers modifier : TextModifiers.values()) {
                if (match.contains(modifier.getKey())) {
                    modifiersCollection.add(modifier);
                }
            }

            token = token.replaceAll(SUFFIX_PATTERN.pattern(), "");
        } else {
            modifiersCollection = Collections.emptyList();
        }

        final PlaceholderParser parser;
        final PlaceholderContext context;
        if (token.startsWith("o:")) {
            if (commandSource instanceof Subject) {
                // option
                parser = this.optionParser;
                context = this.contextForSubjectAndOption((Subject) commandSource, token.substring(2));
            } else {
                return Component.empty();
            }
        } else {
            final String[] s = token.split("\\|", 2);
            final String tokenIn = s[0].toLowerCase();
            final String arg = s.length == 2 ? s[1] : null;
            context = PlaceholderContext.builder()
                    .associatedObject(commandSource)
                    .argumentString(arg)
                    .build();
            parser = this.getParser(tokenIn).orElse(this.emptyParser);
        }

        return new NucleusPlaceholderText(context, parser, modifiersCollection);
    }

    @Override
    public void registerToken(final String tokenName, final PlaceholderParser parser) {
        this.registerToken(tokenName, parser, true, false);
    }

    @Override
    public void registerToken(final String tokenName, final PlaceholderParser parser, final boolean document) {
        this.registerToken(tokenName, parser, document, false);
    }

    private void registerToken(final String tokenName, final PlaceholderParser parser, final boolean document, final boolean isDuplicate) {
        if (SEPARATOR.asPredicate().test(tokenName)) {
            // can't be registered.
            throw new IllegalArgumentException("Tokens must not contain |, :, _ or space characters.");
        }
        final String token = tokenName.toLowerCase();
        if (!this.parsers.containsKey(token)) {
            this.parsers.put(token, new PlaceholderMetadata(token, parser, document, isDuplicate));
        } else {
            throw new IllegalStateException("Token " + tokenName.toLowerCase() + " has already been registered.");
        }
    }

    @Override
    public Optional<PlaceholderParser> getParser(final String token) {
        if (token.contains(":")) {
            return RegistryTypes.PLACEHOLDER_PARSER.get().findValue(ResourceKey.resolve(token));
        }
        final PlaceholderMetadata placeholderMetadata = this.parsers.get(SEPARATOR.split(token.toLowerCase(), 2)[0]);
        if (placeholderMetadata == null) {
            return Optional.empty();
        }
        return Optional.of(placeholderMetadata.getParser());
    }

    @Override
    public PlaceholderParser optionParser() {
        return this.optionParser;
    }

    private PlaceholderContext contextForSubjectAndOption(final Subject subject, final String option) {
        return PlaceholderContext.builder().argumentString(option).associatedObject(subject).build();
    }

    @Override
    public PlaceholderComponent textForSubjectAndOption(final Subject subject, final String option) {
        return PlaceholderComponent.builder()
                .parser(this.optionParser)
                .context(this.contextForSubjectAndOption(subject, option))
                .build();
    }

    @Override public Map<String, PlaceholderMetadata> getNucleusParsers() {
        return Collections.unmodifiableMap(this.parsers);
    }

    // --

    private static ServerWorldProperties getWorld(final PlaceholderContext placeholder) {
        return placeholder.associatedObject()
                .filter(x -> x instanceof Locatable)
                .map(x -> ((Locatable) x).serverLocation().world().properties())
                .orElseGet(Util.defaultWorld()::properties);
    }

}
