/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoDocumentation;
import io.github.nucleuspowered.nucleus.internal.annotations.NoPermissions;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.StandardAbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;

import java.lang.annotation.Annotation;
import java.util.Map;

public class CommandPermissionHandler {

    private final Map<String, PermissionInformation> mssl = Maps.newHashMap();
    private final String prefix;
    private final String base;
    private final String warmup;
    private final String cooldown;
    private final String cost;
    private final String others;

    private final boolean justReturnTrue;

    public CommandPermissionHandler(Class<? extends StandardAbstractCommand> cab, Nucleus plugin) {
        justReturnTrue = cab.isAnnotationPresent(NoPermissions.class);

        // If there are no permissions to assign, we just return true.
        if (justReturnTrue) {
            prefix = "";
            base = "";
            warmup = "";
            cooldown = "";
            cost = "";
            others = "";
            return;
        }

        Permissions c = cab.getAnnotation(Permissions.class);
        if (c == null) {
            c = new Permissions() {
                @Override
                public String[] value() {
                    return new String[0];
                }

                @Override
                public String mainOverride() {
                    return "";
                }

                @Override
                public String prefix() {
                    return "";
                }

                @Override
                public String suffix() {
                    return "";
                }

                @Override
                public boolean supportsSelectors() {
                    return false;
                }

                @Override public boolean supportsOthers() {
                    return false;
                }

                @Override
                public SuggestedLevel suggestedLevel() {
                    return SuggestedLevel.ADMIN;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Permissions.class;
                }
            };
        }

        RegisterCommand co = cab.getAnnotation(RegisterCommand.class);
        String command = co.value()[0];
        StringBuilder sb = new StringBuilder(PermissionRegistry.PERMISSIONS_PREFIX);
        if (!c.prefix().isEmpty()) {
            sb.append(c.prefix()).append(".");
        }

        if (c.mainOverride().isEmpty()) {
            sb.append(command);
        } else {
            sb.append(c.mainOverride());
        }

        sb.append(".");
        if (!c.suffix().isEmpty()) {
            sb.append(c.suffix()).append(".");
        }

        prefix = sb.toString();

        base = prefix + "base";

        if (co.subcommandOf() != StandardAbstractCommand.class) {
            command = String.format("%s %s", co.subcommandOf().getAnnotation(RegisterCommand.class).value()[0], command);
        }

        warmup = prefix + "exempt.warmup";
        cooldown = prefix + "exempt.cooldown";
        cost = prefix + "exempt.cost";
        others = prefix + "others";

        if (!cab.isAnnotationPresent(NoDocumentation.class)) {
            mssl.put(base,
                new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.base", command), c.suggestedLevel()));

            if (c.supportsOthers()) {
                mssl.put(others, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others", co.value()[0]),
                    SuggestedLevel.ADMIN));
            }


            if (!cab.isAnnotationPresent(NoWarmup.class) || cab.getAnnotation(NoWarmup.class).generatePermissionDocs()) {
                mssl.put(warmup, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.exempt.warmup", command),
                    SuggestedLevel.ADMIN));
            }

            if (!cab.isAnnotationPresent(NoCooldown.class)) {
                mssl.put(cooldown, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.exempt.cooldown", command),
                    SuggestedLevel.ADMIN));
            }

            if (!cab.isAnnotationPresent(NoCost.class)) {
                mssl.put(cost, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.exempt.cost", command),
                    SuggestedLevel.ADMIN));
            }
        }

        plugin.getPermissionRegistry().addHandler(cab, this);
    }

    public boolean isPassthrough() {
        return justReturnTrue;
    }

    public String getBase() {
        return base;
    }

    public String getOthers() {
        return others;
    }

    public boolean testBase(Subject src) {
        return test(src, base);
    }

    public boolean testWarmupExempt(Subject src) {
        return test(src, warmup);
    }

    public boolean testCooldownExempt(Subject src) {
        return test(src, cooldown);
    }

    public boolean testCostExempt(Subject src) {
        return test(src, cost);
    }

    public boolean testOthers(Subject src) {
        return test(src, others);
    }

    public void registerPermssionSuffix(String suffix, PermissionInformation pi) {
        this.mssl.put(prefix + suffix, pi);
    }

    public void registerPermssion(String permission, PermissionInformation pi) {
        this.mssl.put(permission, pi);
    }

    public boolean testSuffix(Subject src, String suffix) {
        if (src instanceof User && !(src instanceof Player) && ((User) src).getPlayer().isPresent()) {
            src = ((User) src).getPlayer().get();
        }

        return test(src, prefix + suffix);
    }

    public String getPermissionWithSuffix(String suffix) {
        return prefix + suffix;
    }

    public Map<String, PermissionInformation> getSuggestedPermissions() {
        return ImmutableMap.copyOf(mssl);
    }

    private boolean test(Subject src, String permission) {
        return justReturnTrue || src.hasPermission(permission);
    }
}
