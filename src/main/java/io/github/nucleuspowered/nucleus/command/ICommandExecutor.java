/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.command;

import io.github.nucleuspowered.nucleus.annotationprocessor.Store;
import io.github.nucleuspowered.nucleus.command.annotation.Command;
import io.github.nucleuspowered.nucleus.internal.Constants;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;

import java.util.Optional;

/**
 * Defines the logic of the command.
 *
 * <p>Note that the command must also be annotated with {@link Command}</p>
 *
 * @param <C> The type of {@link CommandSource} that can run this command.
 */
@Store(Constants.COMMAND)
public interface ICommandExecutor<C extends CommandSource> {

    /**
     * The elements that make up the command.
     *
     * @return The elements.
     * @param serviceCollection The {@link INucleusServiceCollection} to use
     */
    default CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[0];
    }

    /**
     * Executes before things like warmups execute. If a result is returned,
     * the command will not execute.
     *
     * <p>Basically, this will run before {@link #execute(ICommandContext)} and
     * has the ability to cancel it.</p>
     *
     * @param context The {@link ICommandContext}
     * @return The result, if any
     */
    default Optional<ICommandResult> preExecute(ICommandContext.Mutable<? extends C> context) throws CommandException {
        return Optional.empty();
    }

    /**
     * The executor of the command.
     *
     * @param context The {@link ICommandContext}
     * @return The result of the command.
     */
    ICommandResult execute(ICommandContext<? extends C> context) throws CommandException;

}
