/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.listener;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;

public class ListenerReloadableWrapper implements IReloadableService.Reloadable {

    private final ListenerBase.Conditional listenerBase;

    public ListenerReloadableWrapper(final ListenerBase.Conditional listenerBase) {
        this.listenerBase = listenerBase;
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        Sponge.eventManager().unregisterListeners(this.listenerBase);
        if (this.listenerBase.shouldEnable(serviceCollection)) {
            Sponge.eventManager().registerListeners(serviceCollection.pluginContainer(), this.listenerBase, Util.LISTENER_LOOKUP);
        }

    }
}
