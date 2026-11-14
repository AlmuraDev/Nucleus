/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldArchetype;
import org.spongepowered.api.world.server.WorldArchetypeType;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;

import java.util.Optional;

@Command(
        aliases = {"create"},
        basePermission = WorldPermissions.BASE_WORLD_CREATE,
        commandDescriptionKey = "world.create",
        parentCommand = WorldCommand.class
)
public class CreateWorldCommand implements ICommandExecutor, IReloadableService.Reloadable {

    @Nullable private Long worldBorderDefault;

    // A dimension (minecraft:overworld, minecraft:the_nether, a datapack's dimension...): its world type plus its generator
    private final Parameter.Value<WorldArchetypeType> worldType =
            Parameter.registryElement(TypeTokens.WORLD_ARCHETYPE_TYPE, RegistryTypes.WORLD_ARCHETYPE_TYPE, "minecraft")
            .key("world type")
            .build();
    private final Parameter.Value<Difficulty> difficultyValue =
            Parameter.registryElement(TypeTokens.DIFFICULTY, RegistryTypes.DIFFICULTY).key("difficulty").build();
    private final Parameter.Value<GameMode> gameMode = NucleusParameters.gameMode("gamemode").build();
    private final Parameter.Value<Long> seedValue = Parameter.longNumber().key("seed").build();
    private final Parameter.Value<Boolean> loadOnStartup = Parameter.bool().key("loadOnStartup").build();
    private final Parameter.Value<Boolean> keepSpawnLoaded = Parameter.bool().key("keepSpawnLoaded").build();
    private final Parameter.Value<Boolean> allowCommands = Parameter.bool().key("allowCommands").build();
    private final Parameter.Value<Boolean> structures = Parameter.bool().key("structures").build();
    private final Parameter.Value<Boolean> bonusChest = Parameter.bool().key("bonusChest").build();
    private final Parameter.Value<String> nameValue = Parameter.string().key("name").build();
    private final Parameter.Value<Boolean> pvpEnabled = Parameter.bool().key("pvp").build();

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of(this.allowCommands, "c", "allowcommands"),
                Flag.of(this.bonusChest, "b", "bonuschest"),
                Flag.of(this.worldType, "w", "worldtype"),
                Flag.of(this.difficultyValue, "d", "difficulty"),
                Flag.of(this.gameMode, "gm", "gamemode"),
                Flag.of(this.seedValue, "s", "seed"),
                Flag.of(this.loadOnStartup, "l", "loadonstartup"),
                Flag.of(this.structures, "st", "structures"),
                Flag.of(this.keepSpawnLoaded, "k", "keepspawnloaded"),
                Flag.of(this.pvpEnabled, "pvp")
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.nameValue
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final String nameInput = context.requireOne(this.nameValue);
        final Optional<WorldArchetypeType> dimensionInput = context.getOne(this.worldType);
        final Optional<GameMode> gamemodeInput = context.getOne(this.gameMode);
        final Optional<Difficulty> difficultyInput = context.getOne(this.difficultyValue);
        final Optional<Long> seedInput = context.getOne(this.seedValue);
        final boolean genStructures = context.getOne(this.structures).orElse(true);
        final boolean loadOnStartup = context.getOne(this.loadOnStartup).orElse(true);
        final boolean keepSpawnLoaded = context.getOne(this.keepSpawnLoaded).orElse(true);
        final boolean allowCommands = context.getOne(this.allowCommands).orElse(true);
        final boolean bonusChest = context.getOne(this.bonusChest).orElse(true);
        final boolean pvp = context.getOne(this.pvpEnabled).orElse(true);

        final ResourceKey projectedKey = ResourceKey.of(context.getServiceCollection().pluginContainer(), nameInput.toLowerCase());
        final WorldManager worldManager = Sponge.server().worldManager();
        if (worldManager.worldExists(projectedKey)) {
            return context.errorResult("command.world.create.exists", nameInput);
        }

        final WorldArchetypeType type = dimensionInput.orElseGet(() ->
                Sponge.server().registry(RegistryTypes.WORLD_ARCHETYPE_TYPE).value(ResourceKey.minecraft("overworld")));
        final ResourceKey typeKey = type.findKey(RegistryTypes.WORLD_ARCHETYPE_TYPE).orElse(ResourceKey.minecraft("overworld"));

        final WorldGenerationConfig.Builder genConfig = WorldGenerationConfig.builder()
                .generateBonusChest(bonusChest)
                .generateStructures(genStructures);
        seedInput.ifPresent(genConfig::seed);

        final WorldArchetype archetype = WorldArchetype.builder()
                .type(type)
                .generationConfig(genConfig.build())
                .build();

        context.sendMessage("command.world.create.begin", nameInput);
        context.sendMessage("command.world.create.newparams",
                typeKey.asString(),
                "-",
                gamemodeInput.flatMap(x -> x.findKey(RegistryTypes.GAME_MODE)).map(ResourceKey::asString)
                        .orElseGet(() -> context.getMessageString("standard.notset")),
                difficultyInput.flatMap(x -> x.findKey(RegistryTypes.DIFFICULTY)).map(ResourceKey::asString)
                        .orElseGet(() -> context.getMessageString("standard.notset")));
        context.sendMessage("command.world.create.newparams2",
                String.valueOf(loadOnStartup),
                String.valueOf(keepSpawnLoaded),
                String.valueOf(genStructures),
                String.valueOf(allowCommands),
                String.valueOf(bonusChest));

        final ServerWorldProperties.LoadOptions options = ServerWorldProperties.LoadOptions.create(archetype, properties -> {
            properties.setLoadOnStartup(loadOnStartup);
            properties.setCommands(allowCommands);
            properties.setPvp(pvp);
            gamemodeInput.ifPresent(properties::setGameMode);
            difficultyInput.ifPresent(properties::setDifficulty);
        });

        worldManager.loadWorld(projectedKey, options).handle((world, exception) -> {
            if (exception != null || world == null || !world.isPresent()) {
                context.getServiceCollection().schedulerService().runOnMainThread(() -> {
                    if (exception != null) {
                        context.sendMessageText(Component.text(String.valueOf(exception.getMessage())));
                    }
                    context.sendMessage("command.world.create.worldfailedtoload", nameInput);
                });
                return null;
            }

            final ServerWorld created = world.get();
            if (this.worldBorderDefault != null && this.worldBorderDefault > 0) {
                final WorldBorder.Builder builder = created.border().toBuilder();
                builder.initialDiameter(this.worldBorderDefault).targetDiameter(this.worldBorderDefault);
                created.setBorder(builder.build());
            }
            context.getServiceCollection().schedulerService().runOnMainThread(
                    () -> context.sendMessage("command.world.create.success", nameInput));
            return null;
        });
        return context.successResult();
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.worldBorderDefault = serviceCollection
                .configProvider()
                .getModuleConfig(WorldConfig.class)
                .getWorldBorderDefault()
                .orElse(null);
    }

}
