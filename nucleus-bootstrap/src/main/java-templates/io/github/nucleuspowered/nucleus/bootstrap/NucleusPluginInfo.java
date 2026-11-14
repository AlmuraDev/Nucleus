/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.bootstrap;

import io.github.nucleuspowered.nucleus.core.IPluginInfo;

/**
 * Contains general information about the plugin.
 *
 * <p>This mostly involves the values that are replaced by Blossom.</p>
 */
@SuppressWarnings("ConstantConditions")
public final class NucleusPluginInfo implements IPluginInfo {
    NucleusPluginInfo() {
    }

    // This isn't going to change now - will break permissions if we have the token.
    public static final String ID = "nucleus";

    public static final String NAME = "@name@";
    public static final String VERSION = "@version@";
    public static final String GIT_HASH = "@gitHash@";
    public static final String URL = "@url@";

    public static final String DESCRIPTION = "@description@";

    public static final String SPONGE_API_VERSION = "@spongeversion@";

    public static final String[] VALID_API_VERSIONS;

    static {
        String[] vers;
        try {
            String s = "@validversions@";
            if (s.isEmpty()) {
                s = SPONGE_API_VERSION;
            }
            vers = s.split(",");
        } catch (final Exception e) {
            vers = new String[] { SPONGE_API_VERSION };
        }
        VALID_API_VERSIONS = vers;
    }

    @Override
    public String id() {
        return NucleusPluginInfo.ID;
    }

    @Override
    public String name() {
        return NucleusPluginInfo.NAME;
    }

    @Override
    public String version() {
        return NucleusPluginInfo.VERSION;
    }

    public String[] validVersions() {
        return NucleusPluginInfo.VALID_API_VERSIONS;
    }

    @Override
    public String description() {
        return NucleusPluginInfo.DESCRIPTION;
    }

    @Override
    public String url() {
        return NucleusPluginInfo.URL;
    }

    @Override
    public String gitHash() {
        return NucleusPluginInfo.GIT_HASH;
    }

}
