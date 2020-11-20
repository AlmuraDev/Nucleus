/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.services;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.mute.NucleusMuteService;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.modules.mute.MuteKeys;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.mute.events.MuteEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Identifiable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;
import org.spongepowered.plugin.PluginContainer;

@APIService(NucleusMuteService.class)
public class MuteHandler implements ContextCalculator<Subject>, NucleusMuteService, ServiceBase {

    private final IMessageProviderService messageProviderService;
    private final IStorageManager storageManager;
    private final PluginContainer pluginContainer;
    private final Map<UUID, Boolean> muteContextCache = new HashMap<>();
    private final Context mutedContext = new Context(NucleusMuteService.MUTED_CONTEXT, "true");

    private boolean globalMuteEnabled = false;
    private final List<UUID> voicedUsers = new ArrayList<>();

    @Inject
    public MuteHandler(final INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.storageManager = serviceCollection.storageManager();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    public void onMute(final Player user) {
        this.getPlayerMuteData(user).ifPresent(x -> onMute(x, user));
    }

    public void onMute(final MuteData md, final Player user) {
        if (md.getRemainingTime().isPresent()) {
            this.messageProviderService.sendMessageTo(user, "mute.playernotify.time",
                    this.messageProviderService.getTimeString(user.getLocale(), md.getRemainingTime().get().getSeconds()));
        } else {
            this.messageProviderService.sendMessageTo(user, "mute.playernotify.standard");
        }
    }

    @Override public boolean isMuted(final User user) {
        return getPlayerMuteData(user).isPresent();
    }

    @Override public Optional<Mute> getPlayerMuteInfo(final User user) {
        return getPlayerMuteData(user).map(x -> x);
    }

    // Internal
    public Optional<MuteData> getPlayerMuteData(final User user) {
        final Optional<MuteData> nu = this.storageManager.getOrCreateUserOnThread(user.getUniqueId()).get(MuteKeys.MUTE_DATA);
        this.muteContextCache.put(user.getUniqueId(), nu.isPresent());
        return nu;
    }

    @Override public boolean mutePlayer(final User user, final String reason, @Nullable final Duration duration, final Cause cause) {
        final UUID first = cause.first(User.class).map(Identifiable::getUniqueId).orElse(Util.CONSOLE_FAKE_UUID);
        return mutePlayer(user, new MuteData(first, reason, duration), cause);
    }

    public boolean mutePlayer(final User user, final MuteData data) {
        return mutePlayer(user, data, CauseStackHelper.createCause((Util.getObjectFromUUID(data.getMuterInternal()))));
    }

    public boolean mutePlayer(final User user, final MuteData data, final Cause cause) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(data);

        final Optional<IUserDataObject> nu = this.storageManager.getUserOnThread(user.getUniqueId());
        if (!nu.isPresent()) {
            return false;
        }

        final Instant time = Instant.now();
        final IUserDataObject u = nu.get();
        final Duration d = data.getRemainingTime().orElse(null);
        if (user.isOnline() && data.getTimeFromNextLogin().isPresent() && !data.getEndTimestamp().isPresent()) {
            data.setEndtimestamp(time.plus(data.getTimeFromNextLogin().get()));
        }

        u.set(MuteKeys.MUTE_DATA, data);
        this.storageManager.saveUser(user.getUniqueId(), u);
        this.muteContextCache.put(user.getUniqueId(), true);
        Sponge.getEventManager().post(new MuteEvent.Muted(
                cause,
                user,
                d,
                Text.of(data.getReason())));
        return true;
    }

    public boolean unmutePlayer(final User user) {
        return unmutePlayer(user, CauseStackHelper.createCause(this.pluginContainer), true);
    }

    @Override public boolean unmutePlayer(final User user, final Cause cause) {
        return unmutePlayer(user, cause, false);
    }

    public boolean unmutePlayer(final User user, final Cause cause, final boolean expired) {
        if (isMuted(user)) {
            final Optional<IUserDataObject> o = this.storageManager.getUserOnThread(user.getUniqueId());
            if (o.isPresent()) {
                final IUserDataObject udo = o.get();
                udo.remove(MuteKeys.MUTE_DATA);
                this.storageManager.saveUser(user.getUniqueId(), udo);
                this.muteContextCache.put(user.getUniqueId(), false);
                Sponge.getEventManager().post(new MuteEvent.Unmuted(
                        cause,
                        user,
                        expired));

                user.getPlayer().ifPresent(x ->
                        this.messageProviderService.sendMessageTo(x, "mute.elapsed"));
                return true;
            }
        }

        return false;
    }

    public boolean isGlobalMuteEnabled() {
        return this.globalMuteEnabled;
    }

    public void setGlobalMuteEnabled(final boolean globalMuteEnabled) {
        if (this.globalMuteEnabled != globalMuteEnabled) {
            this.voicedUsers.clear();
        }

        this.globalMuteEnabled = globalMuteEnabled;
    }

    public boolean isVoiced(final UUID uuid) {
        return this.voicedUsers.contains(uuid);
    }

    public void addVoice(final UUID uuid) {
        this.voicedUsers.add(uuid);
    }

    public void removeVoice(final UUID uuid) {
        this.voicedUsers.remove(uuid);
    }

    @Override public void accumulateContexts(final Subject calculable, final Set<Context> accumulator) {
        if (calculable instanceof User) {
            final UUID u = ((User) calculable).getUniqueId();
            if (this.muteContextCache.computeIfAbsent(u, k -> isMuted((User) calculable))) {
                accumulator.add(this.mutedContext);
            }
        }
    }

    @Override public boolean matches(final Context context, final Subject subject) {
        return context.getKey().equals(NucleusMuteService.MUTED_CONTEXT) && subject instanceof User &&
                this.muteContextCache.computeIfAbsent(((User) subject).getUniqueId(), k -> isMuted((User) subject));
    }

    public boolean isMutedCached(final User x) {
        return this.muteContextCache.containsKey(x.getUniqueId());
    }
}
