/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.listener;

import org.spongepowered.api.effect.VanishState;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.modules.serverlist.config.ServerListConfig;
import io.github.nucleuspowered.nucleus.modules.serverlist.services.ServerListService;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;

public class ServerListListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final ServerListService service;
    private final SecureRandom random = new SecureRandom(); // stops SonarQube complaining

    private final List<NucleusTextTemplate> whitelist = new ArrayList<>();
    private final List<NucleusTextTemplate> messages = new ArrayList<>();
    private boolean hidePlayerCount;
    private boolean hideVanishPlayers;
    private boolean modifyServerList;

    @Inject
    public ServerListListener(final INucleusServiceCollection serviceCollection) {
        this.service = serviceCollection.getServiceUnchecked(ServerListService.class);
    }

    @Listener
    public void onServerListPing(final ClientPingServerEvent event, @Getter("response") final ClientPingServerEvent.Response response) {
        if (this.modifyServerList) {
            List<NucleusTextTemplate> list = null;
            final Optional<Component> ott = this.service.getMessage();

            if (ott.isPresent()) {
                response.setDescription(ott.get());
            } else {
                if (Sponge.server().isWhitelistEnabled() && !this.whitelist.isEmpty()) {
                    list = this.whitelist;
                } else if (!this.messages.isEmpty()) {
                    list = this.messages;
                }

                if (list != null) {
                    final NucleusTextTemplate template = list.get(this.random.nextInt(list.size()));
                    response.setDescription(template.getForObject(Sponge.systemSubject()));
                }
            }
        }

        if (this.hidePlayerCount) {
            response.setHidePlayers(true);
        } else if (this.hideVanishPlayers) {
            final Collection<GameProfile> players = Sponge.server().onlinePlayers().stream()
                    .filter(x -> !x.get(Keys.VANISH_STATE).map(VanishState::invisible).orElse(false))
                    .map(ServerPlayer::profile)
                    .collect(Collectors.toList());

            response.players().ifPresent(y -> {
                y.profiles().clear();
                y.profiles().addAll(players);
                y.setOnline(players.size());
            });
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final ServerListConfig config = serviceCollection.configProvider().getModuleConfig(ServerListConfig.class);
        this.whitelist.clear();
        this.messages.clear();
        config.getWhitelist().stream()
                .map(x -> serviceCollection.textTemplateFactory().createFromAmpersandStringIgnoringExceptions(x).orElse(null))
                .filter(Objects::nonNull)
                .forEach(this.whitelist::add);
        config.getMessages().stream()
                .map(x -> serviceCollection.textTemplateFactory().createFromAmpersandStringIgnoringExceptions(x).orElse(null))
                .filter(Objects::nonNull)
                .forEach(this.messages::add);
        this.hidePlayerCount = config.isHidePlayerCount();
        this.hideVanishPlayers = config.isHideVanishedPlayers();
        this.modifyServerList = config.isModifyServerList();
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(ServerListConfig.class).enableListener();
    }

}
