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
import org.spongepowered.api.entity.living.player.Player;
import java.util.Optional;

@Command(aliases = "set", parentCommand = ExperienceCommand.class,
        basePermission = ExperiencePermissions.BASE_EXP_SET, commandDescriptionKey = "exp.set")
public class SetExperience implements ICommandExecutor {

    private final Parameter.Value<Integer> experienceLevelParameter;
    private final Parameter.Value<Integer> experienceValueParameter;

    @Inject
    public SetExperience(final INucleusServiceCollection serviceCollection) {
        // Levels ("10L", "L10") only, under their own key: it shared the points argument's key, so a plain number
        // was taken as levels, and Parameter.integerNumber() made the client reject "10L"
        this.experienceLevelParameter =
                Parameter.builder(Integer.class).addParser(new ExperienceLevelArgument(serviceCollection))
                        .key("levels").build();
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
        final Player pl = context.getPlayerFromArgs();
        final Optional<ICommandResult> r = ExperienceCommand.checkGameMode(context, pl);
        if (r.isPresent()) {
            return r.get();
        }

        final Optional<Integer> l = context.getOne(this.experienceLevelParameter);
        final DataTransactionResult dtr;
        dtr = l.map(integer -> pl.offer(Keys.EXPERIENCE_LEVEL, integer))
                .orElseGet(() -> pl.offer(Keys.EXPERIENCE, context.requireOne(this.experienceValueParameter)));

        return ExperienceCommand.tellUserAboutExperience(context, pl, dtr.isSuccessful());
    }
}
