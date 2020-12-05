/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ignore.services;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.api.module.ignore.NucleusIgnoreService;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.modules.ignore.IgnoreKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;

@APIService(NucleusIgnoreService.class)
public class IgnoreService implements NucleusIgnoreService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    private final Map<UUID, List<UUID>> ignoredBy = new HashMap<>();

    @Inject
    public IgnoreService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    private void addPlayer(final UUID player, final List<UUID> ignored) {
        this.removePlayer(player);
        this.ignoredBy.put(player, new ArrayList<>(ignored));
    }

    private void removePlayer(final UUID player) {
        this.ignoredBy.remove(player);
    }

    public void ignore(final UUID ignorer, final UUID ignoree) {
        final List<UUID> uuid = this.get(ignorer);
        if (!uuid.contains(ignoree)) {
            uuid.add(ignoree);
            this.serviceCollection.storageManager().getUserService()
                    .getOrNew(ignorer)
                    .thenAccept(x -> x.set(IgnoreKeys.IGNORED, new ArrayList<>(uuid)));
        }
    }

    public void unignore(final UUID ignorer, final UUID ignoree) {
        final List<UUID> uuid = this.get(ignorer);
        if (uuid.contains(ignoree)) {
            uuid.remove(ignoree);
            this.serviceCollection.storageManager().getUserService()
                    .getOrNew(ignorer)
                    .thenAccept(x -> x.set(IgnoreKeys.IGNORED, new ArrayList<>(uuid)));
        }
    }

    @Override
    public boolean isIgnored(final UUID ignorer, final UUID ignoree) {
        return this.get(ignorer).contains(ignoree);
    }

    public List<UUID> getAllIgnored(final UUID ignorer) {
        return ImmutableList.copyOf(this.get(ignorer));
    }

    private List<UUID> get(final UUID player) {
        if (!this.ignoredBy.containsKey(player)) {
            this.addPlayer(player,
                    this.serviceCollection.storageManager().getUserService()
                            .getOnThread(player)
                            .flatMap(x -> x.get(IgnoreKeys.IGNORED))
                            .orElseGet(ImmutableList::of));
        }

        return this.ignoredBy.get(player);
    }

    @Override
    public Collection<UUID> getIgnoredBy(final UUID uuid) {
        return this.getAllIgnored(uuid);
    }

}