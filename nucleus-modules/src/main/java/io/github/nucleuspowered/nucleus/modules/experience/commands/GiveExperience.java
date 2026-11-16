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
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import java.util.Optional;

@Command(
        aliases = "give",
        parentCommand = ExperienceCommand.class,
        basePermission = ExperiencePermissions.BASE_EXP_GIVE,
        commandDescriptionKey = "exp.give"
)
public class GiveExperience implements ICommandExecutor {

    private final Parameter.Value<Integer> experienceLevelParameter;
    private final Parameter.Value<Integer> experienceValueParameter;

    @Inject
    public GiveExperience(final INucleusServiceCollection serviceCollection) {
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
        final Optional<ICommandResult> res = ExperienceCommand.checkGameMode(context, pl);
        if (res.isPresent()) {
            return res.get();
        }

        // Levels ("10L") raise the level; a plain number adds experience points. The points total was offered as the
        // level, so /exp give <player> 10 set the level from a points count (e.g. level 160).
        if (context.hasAny(this.experienceLevelParameter)) {
            final int lvl = pl.get(Keys.EXPERIENCE_LEVEL).orElse(0) + context.requireOne(this.experienceLevelParameter);
            return ExperienceCommand.tellUserAboutExperience(context, pl, pl.offer(Keys.EXPERIENCE_LEVEL, lvl).isSuccessful());
        }

        final int exp = pl.get(Keys.EXPERIENCE).orElse(0) + context.requireOne(this.experienceValueParameter);
        return ExperienceCommand.tellUserAboutExperience(context, pl, pl.offer(Keys.EXPERIENCE, exp).isSuccessful());
    }
}
