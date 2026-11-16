/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command.parameter;

import net.kyori.adventure.text.Component;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads a world from a command: either its full key ({@code terraincontrol:orilla}) or just its name ({@code orilla})
 * when exactly one of the candidate worlds has that name. Worlds live in different namespaces (minecraft, a world
 * generator's, ...), so a bare name was read as {@code minecraft:<name>} and most worlds could only be given by key.
 */
public final class WorldKeys {

    private WorldKeys() {
    }

    public static ResourceKey read(final ArgumentReader.Mutable reader, final Collection<ResourceKey> candidates,
            final Function<String, Component> notFound) throws ArgumentParseException {
        final int start = reader.cursor();
        // A bare name has no namespace, which parseResourceKey() rejects ("Invalid ID")
        final ResourceKey parsed = reader.parseResourceKey("minecraft");
        final String typed = reader.parsed().substring(start).trim();

        if (typed.contains(":")) {
            if (candidates.contains(parsed)) {
                return parsed;
            }
            throw reader.createException(notFound.apply(typed));
        }

        final String name = typed.toLowerCase(Locale.ROOT);
        final List<ResourceKey> matches = candidates.stream()
                .filter(key -> key.value().equalsIgnoreCase(name))
                .sorted()
                .collect(Collectors.toList());
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            throw reader.createException(notFound.apply(typed));
        }
        throw reader.createException(Component.text("More than one world is called \"" + typed + "\": "
                + matches.stream().map(ResourceKey::asString).collect(Collectors.joining(", ")) + ". Give the full name."));
    }

    /**
     * Full keys, plus bare names for the worlds whose name is unique.
     */
    public static List<CommandCompletion> complete(final Collection<ResourceKey> candidates, final String input) {
        final String lowerInput = input.toLowerCase(Locale.ROOT);
        final Map<String, Long> nameCounts = candidates.stream()
                .collect(Collectors.groupingBy(ResourceKey::value, Collectors.counting()));
        final List<CommandCompletion> completions = candidates.stream()
                .filter(key -> nameCounts.get(key.value()) == 1 && key.value().startsWith(lowerInput))
                .map(key -> CommandCompletion.of(key.value()))
                .collect(Collectors.toList());
        candidates.stream()
                .filter(key -> key.asString().startsWith(lowerInput))
                .map(key -> CommandCompletion.of(key.asString()))
                .forEach(completions::add);
        return completions;
    }
}
