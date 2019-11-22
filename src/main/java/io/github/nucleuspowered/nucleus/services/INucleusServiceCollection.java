/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services;

import com.google.inject.ImplementedBy;
import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.services.impl.NucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandElementSupplier;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.services.interfaces.ICompatibilityService;
import io.github.nucleuspowered.nucleus.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.services.interfaces.ICooldownService;
import io.github.nucleuspowered.nucleus.services.interfaces.IEconomyServiceProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageTokenService;
import io.github.nucleuspowered.nucleus.services.interfaces.IModuleDataProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlatformService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerInformationService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextFileControllerCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserCacheService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.services.interfaces.IWarmupService;
import org.slf4j.Logger;
import org.spongepowered.api.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

@ImplementedBy(NucleusServiceCollection.class)
public interface INucleusServiceCollection {

    IMessageProviderService messageProvider();

    IPermissionService permissionService();

    IEconomyServiceProvider economyServiceProvider();

    IWarmupService warmupService();

    ICooldownService cooldownService();

    IUserPreferenceService userPreferenceService();

    IReloadableService reloadableService();

    IPlayerOnlineService playerOnlineService();

    IMessageTokenService messageTokenService();

    IStorageManager storageManager();

    ICommandMetadataService commandMetadataService();

    IPlayerDisplayNameService playerDisplayNameService();

    IModuleDataProvider moduleDataProvider();

    INucleusTeleportService teleportService();

    ICommandElementSupplier commandElementSupplier();

    INucleusTextTemplateFactory textTemplateFactory();

    ITextFileControllerCollection textFileControllerCollection();

    IUserCacheService userCacheService();

    IPlatformService platformService();

    Injector injector();

    PluginContainer pluginContainer();

    ITextStyleService textStyleService();

    IPlayerInformationService playerInformationService();

    IConfigurateHelper configurateHelper();

    ICompatibilityService compatibilityService();

    Logger logger();

    <I, C extends I> void registerService(Class<I> key, C service, boolean rereg);

    <I, C extends I> void registerServiceSupplier(Class<I> key, Supplier<C> service, boolean rereg);

    <I> Optional<I> getService(Class<I> key);

    <I> I getServiceUnchecked(Class<I> key);

    Path configDir();

    Supplier<Path> dataDir();
}
