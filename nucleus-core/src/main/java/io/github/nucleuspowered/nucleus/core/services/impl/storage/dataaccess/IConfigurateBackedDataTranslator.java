/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.configurate.IConfigurateBackedDataObject;
import io.github.nucleuspowered.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;

public interface IConfigurateBackedDataTranslator<R extends IConfigurateBackedDataObject> extends IDataTranslator<R, JsonObject> {

    Gson GSON = new Gson();

    @Override
    default R fromDataAccessObject(final JsonObject object) throws DataLoadException {
        final String json = IConfigurateBackedDataTranslator.GSON.toJson(object);
        final R obj = this.createNew();
        try {
            final ConfigurationNode node = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(json)))
                    .sink(() -> null) // we're not saving
                    .defaultOptions(this.getOptions())
                    .build()
                    .load();
            obj.setBackingNode(node);
        } catch (final ConfigurateException e) {
            throw new DataLoadException("Could not translate Json", e);
        }
        return obj;
    }

    @Override
    default JsonObject toDataAccessObject(final R object) throws DataSaveException {
        // Convert the node straight to Gson's tree. This went through text before (GsonConfigurationLoader#save, then
        // JsonParser), and for an empty map Configurate wrote "{}{}", which can't be parsed: the kits store never
        // saved.
        try {
            final JsonElement json = IConfigurateBackedDataTranslator.toJson(object.getBackingNode());
            return json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
        } catch (final RuntimeException e) {
            throw new DataSaveException("Could not translate " + object.getClass().getSimpleName() + " to Json", e);
        }
    }

    /**
     * The node as Json, as {@link GsonConfigurationLoader} would write it: maps as objects, lists as arrays and
     * scalars (already serialised to strings, numbers and booleans by the type serialisers) as primitives.
     */
    static JsonElement toJson(final ConfigurationNode node) {
        if (node.isMap()) {
            final JsonObject json = new JsonObject();
            node.childrenMap().forEach((key, child) -> json.add(String.valueOf(key), IConfigurateBackedDataTranslator.toJson(child)));
            return json;
        }
        if (node.isList()) {
            final JsonArray json = new JsonArray();
            node.childrenList().forEach(child -> json.add(IConfigurateBackedDataTranslator.toJson(child)));
            return json;
        }
        final Object value = node.rawScalar();
        if (value == null) {
            return JsonNull.INSTANCE;
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        } else if (value instanceof Character) {
            return new JsonPrimitive((Character) value);
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    ConfigurationOptions getOptions();

    default ConfigurationNode createNewNode() {
        return CommentedConfigurationNode.root(this.getOptions());
    }

}
