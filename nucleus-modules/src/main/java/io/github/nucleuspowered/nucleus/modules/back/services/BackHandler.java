/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.services;

import io.github.nucleuspowered.nucleus.api.module.back.NucleusBackService;
import io.github.nucleuspowered.nucleus.datatypes.UUIDTransform;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@APIService(NucleusBackService.class)
public class BackHandler implements NucleusBackService, ServiceBase {

    private final Map<UUID, UUIDTransform> lastLocation = new HashMap<>();
    private final Set<UUID> preventLogLastLocation = new HashSet<>();

    @Override
    public Optional<Transform<World>> getLastLocation(final User user) {
        final UUIDTransform transform = this.lastLocation.get(user.getUniqueId());
        if (transform != null) {
            return transform.loadTransform();
        }

        return Optional.empty();
    }

    @Override
    public void setLastLocation(final User user, final ServerLocation location) {
        this.lastLocation.put(user.getUniqueId(), new UUIDTransform(location));
    }

    @Override
    public void removeLastLocation(final User user) {
        this.lastLocation.remove(user.getUniqueId());
    }

    @Override
    public boolean isLoggingLastLocation(final User user) {
        return !this.preventLogLastLocation.contains(user.getUniqueId());
    }

    @Override
    public void setLoggingLastLocation(final User user, final boolean log) {
        if (log) {
            this.preventLogLastLocation.remove(user.getUniqueId());
        } else {
            this.preventLogLastLocation.add(user.getUniqueId());
        }
    }

}
