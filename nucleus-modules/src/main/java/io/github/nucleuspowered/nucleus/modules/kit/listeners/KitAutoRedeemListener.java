/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.listeners;

import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.List;

import com.google.inject.Inject;

public class KitAutoRedeemListener implements ListenerBase.Conditional, IReloadableService.Reloadable {

    private final KitService kitService;
    private final Logger logger;

    private boolean mustGetAll;
    private boolean logAutoRedeem = false;

    @Inject
    public KitAutoRedeemListener(final INucleusServiceCollection serviceCollection) {
        this.kitService = serviceCollection.getServiceUnchecked(KitService.class);
        this.logger = serviceCollection.logger();
    }

    // TODO: Replace
    @Listener
    public void onPlayerJoin(final ClientConnectionEvent.Join event, @Root final Player player) {
        final List<Kit> autoRedeemable = this.kitService.getAutoRedeemable();
        final String name = "[Kit Auto Redeem - " + player.getName() + "]: ";
        for (final Kit kit : autoRedeemable) {
            final String permission = KitPermissions.getKitPermission(kit.getName().toLowerCase());
            final String kitName = kit.getName();
            if (kit.ignoresPermission()) {
                log(name + kitName + " - permission check bypassed.");
            } else if (player.hasPermission(permission)) {
                log(name  + kitName + " - permission check " + permission + " passed.");
            } else {
                continue;
            }

            // Redeem kit in the normal way.
            final KitRedeemResult redeemResult = this.kitService.redeemKit(kit, player, true, true, this.mustGetAll, false);
            if (redeemResult.isSuccess()) {
                log(name + kitName + " - kit redeemed.");
            } else if (this.logAutoRedeem) {
                this.logger.error(name + kitName + " - kit could not be redeemed.", redeemResult.getStatus().name());
            }
        }
    }

    @Override public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(KitConfig.class).isEnableAutoredeem();
    }

    private void log(final String message) {
        if (this.logAutoRedeem) {
            this.logger.info(message);
        }
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final KitConfig kca = serviceCollection.configProvider().getModuleConfig(KitConfig.class);
        this.mustGetAll = kca.isMustGetAll();
        this.logAutoRedeem = kca.isLogAutoredeem();
    }
}
