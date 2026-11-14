/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Loads Nucleus' UTF-8 message bundles from its own jar.
 *
 * <p>Sponge on NeoForge loads plugins as named modules, where {@link ResourceBundle#getBundle(String, Locale,
 * ResourceBundle.Control)} throws "ResourceBundle.Control not supported in named modules". This reads the
 * properties files through this class (so from Nucleus' own module) and chains them most specific first, the way
 * {@code getBundle} does: {@code messages_de_DE} falls back to {@code messages_de}, then {@code messages}.</p>
 */
public final class UTF8Control {

    private UTF8Control() {}

    /**
     * @param baseName the bundle name with dots, e.g. {@code data.plugin-nucleus.messages}
     * @param locale the locale to load; {@link Locale#ROOT} loads only the base file
     * @throws MissingResourceException if not even the base file exists
     */
    public static ResourceBundle getBundle(final String baseName, final Locale locale) {
        final String path = "/" + baseName.replace('.', '/');
        ResourceBundle parent = null;
        for (final String suffix : suffixes(locale)) {
            final ResourceBundle bundle = load(path + suffix + ".properties", parent);
            if (bundle != null) {
                parent = bundle;
            }
        }
        if (parent == null) {
            throw new MissingResourceException("Can't find bundle " + baseName, UTF8Control.class.getName(), baseName);
        }
        return parent;
    }

    // Least specific first: "", "_de", "_de_DE", "_de_DE_variant"
    private static List<String> suffixes(final Locale locale) {
        final List<String> suffixes = new ArrayList<>();
        suffixes.add("");
        if (!locale.getLanguage().isEmpty()) {
            suffixes.add("_" + locale.getLanguage());
            if (!locale.getCountry().isEmpty()) {
                suffixes.add("_" + locale.getLanguage() + "_" + locale.getCountry());
                if (!locale.getVariant().isEmpty()) {
                    suffixes.add("_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant());
                }
            }
        }
        return suffixes;
    }

    private static ResourceBundle load(final String resource, final ResourceBundle parent) {
        try (final InputStream stream = UTF8Control.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            try (final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new ChainedBundle(reader, parent);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }

    private static final class ChainedBundle extends PropertyResourceBundle {

        ChainedBundle(final Reader reader, final ResourceBundle parent) throws IOException {
            super(reader);
            if (parent != null) {
                this.setParent(parent);
            }
        }
    }

}
