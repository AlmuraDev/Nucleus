/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.tests;

import io.github.nucleuspowered.nucleus.core.Util;
import org.junit.Ignore;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.spongepowered.api.world.border.WorldBorder;

import org.spongepowered.api.util.Ticks;
import java.time.Duration;
import java.util.Arrays;

public class UtilTests {

    @SuppressWarnings("CanBeFinal")
    @RunWith(Parameterized.class)
    public static class WorldBorderTests {
        @Parameterized.Parameters(name = "{index}: Co-ords ({0}, {1}, {2}), border centre ({3}, {4}, {5}), diameter: {6}, expecting {7}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {0, 0, 0, 0, 0, 0, 10, true},
                    {20, 0, 0, 0, 0, 0, 10, false},
                    {0, 20, 0, 0, 0, 0, 10, true},
                    {20, 0, 20, 0, 0, 0, 10, false},
                    {0, 0, 20, 0, 0, 0, 10, false},
                    {4, 0, 4, 0, 0, 0, 10, true},
                    {5, 0, 5, 0, 0, 0, 10, true},
                    {5, 0, 5, 0, 20, 0, 10, true},
                    {6, 0, 5, 0, 20, 0, 10, false},
                    {5, 0, 5, 500, 0, 0, 10, false},
                    {499, 0, 5, 500, 0, 0, 10, true},
                    {499, 0, 500, 500, 0, 0, 10, false}
            });
        }

        @Parameterized.Parameter()
        public double x;

        @Parameterized.Parameter(1)
        public double y;

        @Parameterized.Parameter(2)
        public double z;

        @Parameterized.Parameter(3)
        public double borderX;

        @Parameterized.Parameter(4)
        public double borderY;

        @Parameterized.Parameter(5)
        public double borderZ;

        @Parameterized.Parameter(6)
        public double dia;

        @Parameterized.Parameter(7)
        public boolean result;

        private WorldBorder getBorder() {
            return new WorldBorder() {

                @Override public Vector2d center() {
                    return null;
                }

                @Override public double diameter() {
                    return 0;
                }

                @Override public double targetDiameter() {
                    return 0;
                }

                @Override public Ticks timeUntilTargetDiameter() {
                    return null;
                }

                @Override public double safeZone() {
                    return 0;
                }

                @Override public double damagePerBlock() {
                    return 0;
                }

                @Override public Ticks warningTime() {
                    return null;
                }

                @Override public int warningDistance() {
                    return 0;
                }
            };
        }

        @Test
        @Ignore
        public void testInWorldBorder() {
            final WorldBorder wb = this.getBorder();
            final ServerWorld world = Mockito.mock(ServerWorld.class);
            Mockito.when(world.properties().worldBorder()).thenReturn(wb);

            final ServerLocation lw = Mockito.mock(ServerLocation.class);
            Mockito.when(lw.world()).thenReturn(world);
            Mockito.when(lw.position()).thenReturn(new Vector3d(this.x, this.y, this.z));
            Assert.assertEquals(this.result, Util.isLocationInWorldBorder(lw));
        }
    }
}
