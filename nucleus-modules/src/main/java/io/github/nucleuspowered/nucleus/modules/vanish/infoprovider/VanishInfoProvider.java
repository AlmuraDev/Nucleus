/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.infoprovider;

import org.spongepowered.api.effect.VanishState;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;

public class VanishInfoProvider implements NucleusProvider.Permission {

    @Override public String getCategory() {
        return "vanish";
    }

    @Override public String permission() {
        return VanishPermissions.VANISH_SEE;
    }

    @Override
    public @Nullable Component getText(final User user, final CommandCause source, final INucleusServiceCollection serviceCollection) {
        final IMessageProviderService providerService = serviceCollection.messageProvider();
        final boolean isVanished = user.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false);
        final String yesNo = providerService.getMessageString(isVanished ? "standard.yesno.true" : "standard.yesno.false");
        return providerService.getMessageFor(source.audience(), "seen.vanish", yesNo);
    }

}
