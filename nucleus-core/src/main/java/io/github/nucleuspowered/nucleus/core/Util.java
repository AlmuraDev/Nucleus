/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core;

import java.util.Locale;
import org.spongepowered.api.ResourceKey;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import org.spongepowered.api.network.ServerConnectionState;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.world.DefaultWorldKeys;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public final class Util {

    private Util() {
    }

    public static final DateTimeFormatter FULL_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withZone(ZoneId.systemDefault());

    public static final String USERNAME_REGEX_STRING = "[0-9a-zA-Z_]{3,16}";
    public static final Pattern USERNAME_REGEX_PATTERN = Pattern.compile(USERNAME_REGEX_STRING);

    public static final UUID CONSOLE_FAKE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static String getNameOrUnkown(final ICommandContext context, final GameProfile profile) {
        return profile.name().orElse(
                context.getServiceCollection().messageProvider().getMessageString("standard.unknown"));
    }

    public static String getTimeFromDayTime(final IMessageProviderService messageProviderService, final MinecraftDayTime dayTime) {
        final NumberFormat m = NumberFormat.getIntegerInstance();
        m.setMinimumIntegerDigits(2);

        int hours = dayTime.hour();
        final int mins = dayTime.minute();
        if (hours < 12) {
            final long ahours = hours == 0 ? 12 : hours;
            return messageProviderService.getMessageString("standard.time.am", ahours, hours, m.format(mins));
        } else {
            hours -= 12;
            final long ahours = hours == 0 ? 12 : hours;
            return messageProviderService.getMessageString("standard.time.pm", ahours, hours, m.format(mins));
        }
    }

    /**
     * Gets a key from a map based on a case insensitive key.
     *
     * @param map The {@link Map} to check.
     * @param key The {@link String} key.
     * @return An {@link Optional}, which contains the key if it exists.
     */
    public static Optional<String> getKeyIgnoreCase(final Map<String, ?> map, final String key) {
        return getKeyIgnoreCase(map.keySet(), key);
    }

    /**
     * Gets a key from a map based on a case insensitive key.
     *
     * @param collection The {@link Collection} to check.
     * @param key The {@link String} key.
     * @return An {@link Optional}, which contains the key if it exists.
     */
    public static Optional<String> getKeyIgnoreCase(final Collection<String> collection, final String key) {
        return collection.stream().filter(x -> x.equalsIgnoreCase(key)).findFirst();
    }

    /**
     * Gets a value from a map based on a case insensitive key.
     *
     * @param map The {@link Map} to check.
     * @param key The {@link String} key.
     * @param <T> The type of values in the map.
     * @return An {@link Optional}, which contains a value if the key exists in some case form.
     */
    public static <T> Optional<T> getValueIgnoreCase(final Map<String, T> map, final String key) {
        return map.entrySet().stream().filter(x -> x.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue).findFirst();
    }

    /**
     * Tests to see if the supplied {@link Location} is within the world's {@link org.spongepowered.api.world.border.WorldBorder}
     *
     * @param location The {@link Location} to test.
     * @return <code>true</code> if the location is within the border.
     */
    public static boolean isLocationInWorldBorder(final ServerLocation location) {
        return isLocationInWorldBorder(location.position(), location.world());
    }

    public static boolean isLocationInWorldBorder(final Vector3d location, final ServerWorld world) {

        // Diameter, not radius - we'll want the radius later. We use long, we want the floor!
        final long radius = (long)Math.floor(world.properties().worldBorder().diameter() / 2.0);

        // We get the current position and subtract the border centre. This gives us an effective distance from the
        // centre in all three dimensions. We just care about the magnitude in the x and z directions, so we get the
        // positive amount.
        final Vector2d displacement = location.toVector2(true).sub(world.properties().worldBorder().center()).abs();

        // Check that we're not too far out.
        return !(displacement.x() > radius || displacement.y() > radius);
    }

    public static void compressAndDeleteFile(final Path from) throws IOException {
        // Get the file.
        if (Files.exists(from)) {
            final Path to = Paths.get(from.toString() + ".gz");
            try (final OutputStream os = new GZIPOutputStream(new FileOutputStream(to.toFile()))) {
                Files.copy(from, os);
                os.flush();
                Files.delete(from);
            }

        }

    }

    public static PaginationList.Builder getPaginationBuilder(final Audience source) {
        return getPaginationBuilder(source instanceof ServerPlayer);
    }

    public static PaginationList.Builder getPaginationBuilder(final boolean isPlayer) {
        final PaginationList.Builder plb = Sponge.serviceProvider().paginationService().builder();
        if (!isPlayer) {
            plb.linesPerPage(-1);
        }

        return plb;
    }

    public static ItemStack dropItemOnFloorAtLocation(final ItemStackSnapshot itemStackSnapshotToDrop, final ServerLocation location) {
        return dropItemOnFloorAtLocation(itemStackSnapshotToDrop, location.world(), location.position());
    }

    /**
     * Lookup for {@code EventManager#registerListeners(PluginContainer, Object, Lookup)}. Sponge skips listeners in a
     * named module (how SpongeNeo loads plugins) when it has no lookup for them, with only a "No lookup found" warning.
     */
    public static final MethodHandles.Lookup LISTENER_LOOKUP = MethodHandles.lookup();

    /**
     * Copies one of Nucleus' bundled default files ({@code data/plugin-nucleus/<name>} in the jar) to {@code target}.
     *
     * @return false if there is no such bundled file
     */
    public static boolean copyBundledFile(final String name, final Path target) throws IOException {
        try (final InputStream stream = Util.class.getResourceAsStream("/data/plugin-nucleus/" + name)) {
            if (stream == null) {
                return false;
            }
            Files.copy(stream, target);
            return true;
        }
    }

    /**
     * The part of a world's key used in per-world permissions ({@code nucleus.worlds.<world>} and the like): the key's
     * value, lower case, e.g. {@code orilla} for {@code sponge:orilla}. The full key's ':' can't be passed to NeoForge
     * permission handlers such as LuckPerms, and the value matches the world-name nodes granted on older servers.
     */
    public static String worldPermissionSuffix(final ResourceKey worldKey) {
        return worldKey.value().toLowerCase(Locale.ROOT);
    }

    /** The player's ping in milliseconds, or 0 when it is not known (for example while still logging in). */
    public static int latency(final ServerPlayer player) {
        return player.connection().state()
                .filter(ServerConnectionState.Game.class::isInstance)
                .map(state -> ((ServerConnectionState.Game) state).latency())
                .orElse(0);
    }

    /** The server's default world. {@code WorldManager#defaultWorld()} was removed after API 8. */
    public static ServerWorld defaultWorld() {
        return Sponge.server().worldManager().world(DefaultWorldKeys.DEFAULT)
                .orElseThrow(() -> new IllegalStateException("The default world is not loaded"));
    }

    public static ItemStack dropItemOnFloorAtLocation(final ItemStackSnapshot itemStackSnapshotToDrop, final ServerWorld world,
            final Vector3d position) {
        final Entity entityToDrop = world.createEntity(EntityTypes.ITEM.get(), position);
        entityToDrop.offer(Keys.ITEM_STACK_SNAPSHOT, itemStackSnapshotToDrop);
        world.spawnEntity(entityToDrop);
        return itemStackSnapshotToDrop.asMutableCopy();
    }

    public static Inventory getStandardInventory(final Carrier player) {
        return player.inventory()
                .query(QueryTypes.PLAYER_PRIMARY_HOTBAR_FIRST.get().toQuery());
    }

    public static boolean hasPlayedBeforeSponge(final User player) {
        final Instant instant = player.get(Keys.FIRST_DATE_JOINED).orElseGet(Instant::now);
        final Instant next = Instant.now().plus(5, ChronoUnit.SECONDS);
        return instant.isAfter(next);
    }

}
