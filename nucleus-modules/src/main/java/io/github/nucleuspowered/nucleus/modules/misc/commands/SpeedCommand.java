/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.modules.misc.config.MiscConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EssentialsEquivalent(value = {"speed", "flyspeed", "walkspeed", "fspeed", "wspeed"}, isExact = false,
    notes = "This command either uses your current state or a specified argument to determine whether to alter fly or walk speed.")
@Command(
        aliases = "speed",
        basePermission = MiscPermissions.BASE_SPEED,
        commandDescriptionKey = "speed",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MiscPermissions.EXEMPT_WARMUP_SPEED),
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MiscPermissions.EXEMPT_COOLDOWN_SPEED),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MiscPermissions.EXEMPT_COST_SPEED)
        },
        associatedPermissions = {
                MiscPermissions.SPEED_EXEMPT_MAX,
                MiscPermissions.OTHERS_SPEED
        }
)
public class SpeedCommand implements ICommandExecutor, IReloadableService.Reloadable { //extends AbstractCommand.SimpleTargetOtherPlayer

    /**
     * As the standard flying speed is 0.05 and the standard walking speed is
     * 0.1, we multiply it by 20 and use integers. Standard walking speed is
     * therefore 2, standard flying speed - 1.
     */
    public static final int multiplier = 20;
    private int maxSpeed = 5;

    private final Parameter.Value<SpeedType> speedTypeParameter;
    private final Parameter.Value<Double> speed = Parameter.builder(Double.class)
            .addParser(VariableValueParameters.doubleRange().min(0.0).build())
            .key("speed")
            .build();
    private final Parameter.Value<Boolean> reset = Parameter.builder(Boolean.class)
            .key("reset")
            // A literal needs a value to return, or matching it counts as a failed argument and "reset" was rejected
            .addParser(VariableValueParameters.literalBuilder(Boolean.class).literal(Collections.singleton("reset")).returnValue(true).build())
            .build();

    @Inject
    public SpeedCommand() {
        final Map<String, SpeedType> keysMap = new HashMap<>();
        keysMap.put("fly", SpeedType.FLYING);
        keysMap.put("flying", SpeedType.FLYING);
        keysMap.put("f", SpeedType.FLYING);

        keysMap.put("walk", SpeedType.WALKING);
        keysMap.put("w", SpeedType.WALKING);

        this.speedTypeParameter = Parameter.builder(SpeedType.class)
                .key("type")
                .optional()
                .addParser(VariableValueParameters.staticChoicesBuilder(SpeedType.class).addChoices(keysMap).build())
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherPlayerPermissionElement(MiscPermissions.OTHERS_SPEED),
                this.speedTypeParameter,
                Parameter.firstOfBuilder(this.speed).or(this.reset).optional().build()
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Player pl = context.getPlayerFromArgs();
        final SpeedType key = context.getOne(this.speedTypeParameter)
                .orElseGet(() -> pl.get(Keys.IS_FLYING).orElse(false) ? SpeedType.FLYING : SpeedType.WALKING);
        final Double speed = context.getOne(this.speed).orElseGet(() -> {
            if (context.hasAny(this.reset)) {
                return key == SpeedType.WALKING ? 2.0d : 1.0d;
            }

            return null;
        });

        if (speed == null) {
            final Component t = LinearComponents.linear(
                    context.getMessage("command.speed.walk"),
                    Component.space(),
                    Component.text(pl.get(Keys.WALKING_SPEED).orElse(0.1d) * SpeedCommand.multiplier, NamedTextColor.YELLOW),
                    Component.text(", ", NamedTextColor.GREEN),
                    context.getMessage("command.speed.flying").color(NamedTextColor.GREEN),
                    Component.text(pl.get(Keys.FLYING_SPEED).orElse(0.1d) * SpeedCommand.multiplier, NamedTextColor.YELLOW),
                    Component.text(".", NamedTextColor.GREEN));

            context.sendMessageText(t);

            // Don't trigger cooldowns
            return context.failResult();
        }

        if (speed < 0) {
            return context.errorResult("command.speed.negative");
        }

        if (!context.isConsoleAndBypass() && !context.testPermission(MiscPermissions.SPEED_EXEMPT_MAX) && this.maxSpeed < speed) {
            return context.errorResult("command.speed.max", String.valueOf(this.maxSpeed));
        }

        final DataTransactionResult dtr = pl.offer(key.speedKey, speed / (double) multiplier);

        if (dtr.isSuccessful()) {
            // "Your speed..." goes to the player whose speed changed, not to whoever ran the command
            context.sendMessageTo(pl, "command.speed.success.base", key.name, String.valueOf(speed));

            if (!context.is(pl)) {
                context.sendMessage("command.speed.success.other", pl.name(), key.name, String.valueOf(speed));
            }

            return context.successResult();
        }

        return context.errorResult("command.speed.fail", key.name);
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.maxSpeed = serviceCollection.configProvider().getModuleConfig(MiscConfig.class).getMaxSpeed();
    }

    private enum SpeedType {
        WALKING(Keys.WALKING_SPEED, "loc:standard.walking"),
        FLYING(Keys.FLYING_SPEED, "loc:standard.flying");

        final Key<Value<Double>> speedKey;
        final String name;

        SpeedType(final Key<Value<Double>> speedKey, final String name) {
            this.speedKey = speedKey;
            this.name = name;
        }
    }
}
