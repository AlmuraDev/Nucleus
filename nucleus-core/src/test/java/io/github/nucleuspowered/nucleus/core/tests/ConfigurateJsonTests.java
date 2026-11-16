/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IConfigurateBackedDataTranslator;
import org.junit.Assert;
import org.junit.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

/**
 * Saving converts Configurate nodes to Json with {@link IConfigurateBackedDataTranslator#toJson(ConfigurationNode)};
 * loading reads that Json back with {@link GsonConfigurationLoader}. The two must agree.
 */
public class ConfigurateJsonTests {

    private static final ConfigurationOptions OPTIONS = ConfigurationOptions.defaults().implicitInitialization(false);

    private static ConfigurationNode load(final JsonElement json) throws Exception {
        final String text = json.toString();
        return GsonConfigurationLoader.builder()
                .source(() -> new BufferedReader(new StringReader(text)))
                .defaultOptions(ConfigurateJsonTests.OPTIONS)
                .build()
                .load();
    }

    @Test
    public void anEmptyMapIsAnEmptyObject() throws Exception {
        // The kits store with no kits: the old text round trip produced "{}{}", which could not be parsed
        final CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigurateJsonTests.OPTIONS);
        node.set(Collections.emptyMap());
        final JsonElement json = IConfigurateBackedDataTranslator.toJson(node);
        Assert.assertTrue(json.isJsonObject());
        Assert.assertEquals(0, json.getAsJsonObject().size());
    }

    @Test
    public void anEmptyNodeIsNotAnObject() {
        // The translator turns this into an empty JsonObject
        final CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigurateJsonTests.OPTIONS);
        Assert.assertFalse(IConfigurateBackedDataTranslator.toJson(node).isJsonObject());
    }

    @Test
    public void nestedDataSurvivesTheRoundTrip() throws Exception {
        final CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigurateJsonTests.OPTIONS);
        node.node("data_version").set(2);
        node.node("name").set("Dev2");
        node.node("flying").set(true);
        node.node("speed").set(0.25d);
        node.node("homes", "test", "world").set("minecraft:overworld");
        node.node("homes", "test", "position").set(Arrays.asList(1215.5d, 135d, 853.25d));
        node.node("kits", "starter").set(Collections.emptyMap());

        final JsonObject json = IConfigurateBackedDataTranslator.toJson(node).getAsJsonObject();
        Assert.assertEquals(2, json.get("data_version").getAsInt());
        Assert.assertEquals("Dev2", json.get("name").getAsString());
        Assert.assertTrue(json.get("flying").getAsBoolean());

        final ConfigurationNode loaded = ConfigurateJsonTests.load(json);
        Assert.assertEquals(2, loaded.node("data_version").getInt());
        Assert.assertEquals("Dev2", loaded.node("name").getString());
        Assert.assertTrue(loaded.node("flying").getBoolean());
        Assert.assertEquals(0.25d, loaded.node("speed").getDouble(), 0d);
        Assert.assertEquals("minecraft:overworld", loaded.node("homes", "test", "world").getString());
        Assert.assertEquals(3, loaded.node("homes", "test", "position").childrenList().size());
        Assert.assertEquals(853.25d, loaded.node("homes", "test", "position").childrenList().get(2).getDouble(), 0d);
        Assert.assertTrue(loaded.node("kits", "starter").isMap());
    }
}
