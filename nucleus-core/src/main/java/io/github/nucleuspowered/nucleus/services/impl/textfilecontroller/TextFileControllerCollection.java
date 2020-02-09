/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.textfilecontroller;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.io.TextFileController;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextFileControllerCollection;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Singleton
public class TextFileControllerCollection implements ITextFileControllerCollection, IReloadableService.Reloadable {

    private final Map<String, TextFileController> textFileControllers = Maps.newHashMap();

    @Inject
    public TextFileControllerCollection(INucleusServiceCollection serviceCollection) {
        serviceCollection.reloadableService().registerReloadable(this);
    }

    @Override public Optional<TextFileController> get(String key) {
        return Optional.ofNullable(this.textFileControllers.get(key));
    }

    @Override public void register(String key, TextFileController controller) {
        this.textFileControllers.put(key, controller);
    }

    @Override public void remove(String key) {
        this.textFileControllers.remove(key);
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        for (TextFileController textFileController : this.textFileControllers.values()) {
            try {
                textFileController.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
