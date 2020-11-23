/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.modules.admin.parameter.AdminParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Command(aliases = "killentity", basePermission = AdminPermissions.BASE_KILLENTITY, commandDescriptionKey = "killentity",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_KILLENTITY),
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_KILLENTITY),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_KILLENTITY)
        })
public class KillEntityCommand implements ICommandExecutor {

    private final Parameter.Value<Integer> radius = Parameter.builder(Integer.class).setKey("radius").parser(
            VariableValueParameters.integerRange().setMin(0).setMax(Integer.MAX_VALUE).build()
    ).build();

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.builder().alias("r").setParameter(this.radius).build(),
                Flag.builder().alias("w").setParameter(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY).build()
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                AdminParameters.ENTITY_PARAMETER
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final CommandCause src = context.getCause();
        if (!(src.getLocation().isPresent()) && context.hasAny(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)) {
            // We can't do that.
            return context.errorResult("command.killentity.commandsourceradius");
        }

        if (context.hasAny(this.radius) && context.hasAny(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)) {
            // Can't do that, either.
            return context.errorResult("command.killentity.radiusworld");
        }

        final Set<Entity> currentEntities = new HashSet<>();
        if (context.hasAny(this.radius)) {
            final Locatable l = ((Locatable) src);
            final int r = context.requireOne(this.radius);
            l.getServerLocation().getWorld().getNearbyEntities(l.getServerLocation().getPosition(), r);
        } else {
            final WorldProperties worldProperties;
            if (context.hasAny(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY)) {
                worldProperties = context.requireOne(CommonParameters.ONLINE_WORLD_PROPERTIES_ONLY);
            } else {
                worldProperties = ((Locatable) src).getServerLocation().getWorld().getProperties();
            }
            worldProperties.getWorld().ifPresent(x -> currentEntities.addAll(x.getEntities()));
        }

        final Predicate<Entity> entityPredicate = context.getAll(AdminParameters.ENTITY_PARAMETER)
                .stream()
                .map(x -> (Predicate<Entity>) x)
                .reduce(Predicate::or)
                .orElseThrow(() -> context.createException("command.killentity.noselection"));
        final Set<Entity> toKill = currentEntities.stream().filter(entityPredicate).collect(Collectors.toSet());
        if (toKill.isEmpty()) {
            return context.errorResult("command.killentity.nothing");
        }

        final int killCount = toKill.size();
        toKill.forEach(x -> {
            x.offer(Keys.HEALTH, 0d);
            x.remove();
        });

        context.sendMessage("command.killentity.success", String.valueOf(killCount));
        return context.successResult();
    }


}
