/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.experience.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.experience.ExperiencePermissions;
import io.github.nucleuspowered.nucleus.modules.experience.parameter.ExperienceLevelArgument;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;

@Command(aliases = "take",
        parentCommand = ExperienceCommand.class,
        basePermission = ExperiencePermissions.BASE_EXP_TAKE,
        commandDescriptionKey = "exp.take")
public class TakeExperience implements ICommandExecutor {

    private final Parameter.Value<Integer> experienceLevelParameter;
    private final Parameter.Value<Integer> experienceValueParameter;

    @Inject
    public TakeExperience(final INucleusServiceCollection serviceCollection) {
        this.experienceLevelParameter =
                Parameter.integerNumber().addParser(new ExperienceLevelArgument(serviceCollection))
                        .key("experience").build();
        this.experienceValueParameter =
                Parameter.integerNumber().addParser(VariableValueParameters.integerRange().min(0).max(Integer.MAX_VALUE).build())
                        .key("experience").build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.OPTIONAL_ONE_PLAYER,
                Parameter.firstOf(this.experienceLevelParameter, this.experienceValueParameter)
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer pl = context.getPlayerFromArgs();
        final Optional<ICommandResult> res = ExperienceCommand.checkGameMode(context, pl);
        if (res.isPresent()) {
            return res.get();
        }


        if (context.hasAny(this.experienceLevelParameter)) {
            final int currentLevel = pl.get(Keys.EXPERIENCE_LEVEL).orElse(0);
            final int levelReduction = context.requireOne(this.experienceLevelParameter);

            // If this will take us down to below zero, we just let this continue to the return line. Else...
            if (currentLevel >= levelReduction) {
                final int betweenLevelExp = pl.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).orElse(0);
                final int extra = pl.get(Keys.EXPERIENCE_SINCE_LEVEL).orElse(0);
                DataTransactionResult result = pl.offer(Keys.EXPERIENCE_LEVEL, currentLevel - levelReduction);
                if (result.isSuccessful()) {
                    result = pl.offer(Keys.EXPERIENCE_SINCE_LEVEL, Math.min(extra, betweenLevelExp));
                }
                return ExperienceCommand.tellUserAboutExperience(context, pl, result.isSuccessful());
            }
        }

        final int extra = context.requireOne(this.experienceValueParameter);
        final int exp = pl.get(Keys.EXPERIENCE).orElse(0) - extra;
        return ExperienceCommand.tellUserAboutExperience(context, pl, pl.offer(Keys.EXPERIENCE_LEVEL, Math.max(0, exp)).isSuccessful());
    }
}
