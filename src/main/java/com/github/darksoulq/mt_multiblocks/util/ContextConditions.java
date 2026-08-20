package com.github.darksoulq.mt_multiblocks.util;

import com.github.darksoulq.mt_multiblocks.BlueprintContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class ContextConditions {

    private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final Map<BlockType, Block> BLOCK_CACHE = new ConcurrentHashMap<>();

    private static final Map<Integer, long[]> CUBE_OFFSETS = new ConcurrentHashMap<>();
    private static final Map<Integer, long[]> SPHERE_OFFSETS = new ConcurrentHashMap<>();

    private ContextConditions() {}

    private static long[] getCubeOffsets(int radius) {
        if (radius < 1) return new long[0];
        return CUBE_OFFSETS.computeIfAbsent(radius, r -> {
            int size = (2 * r + 1) * (2 * r + 1) * (2 * r + 1);
            long[] offsets = new long[size];
            int index = 0;
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        offsets[index++] = (x & 0xFFFFL) | ((y & 0xFFFFL) << 16) | ((z & 0xFFFFL) << 32);
                    }
                }
            }
            return offsets;
        });
    }

    private static long[] getSphereOffsets(int radius) {
        if (radius < 1) return new long[0];
        return SPHERE_OFFSETS.computeIfAbsent(radius, r -> {
            int rSq = r * r;
            long[] temp = new long[(2 * r + 1) * (2 * r + 1) * (2 * r + 1)];
            int index = 0;
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (x * x + y * y + z * z <= rSq) {
                            temp[index++] = (x & 0xFFFFL) | ((y & 0xFFFFL) << 16) | ((z & 0xFFFFL) << 32);
                        }
                    }
                }
            }
            long[] result = new long[index];
            System.arraycopy(temp, 0, result, 0, index);
            return result;
        });
    }

    public static ServerLevel getServerLevel(BlueprintContext context) {
        return ((CraftWorld) context.getCoreLocation().getWorld()).getHandle();
    }

    public static Block toNMS(BlockType type) {
        return BLOCK_CACHE.computeIfAbsent(type, k -> BuiltInRegistries.BLOCK.getValue(Identifier.parse(k.getKey().toString())));
    }

    private static boolean isChunkLoaded(ServerLevel level, int x, int z) {
        return level.getChunkSource().hasChunk(x >> 4, z >> 4);
    }

    public static boolean isBlockAtOffset(BlueprintContext context, int dx, int dy, int dz, BlockType type) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int x = loc.getBlockX() + dx;
        int z = loc.getBlockZ() + dz;

        if (!isChunkLoaded(level, x, z)) return false;

        Block expected = toNMS(type);
        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, loc.getBlockY() + dy, z);
        return level.getBlockState(pos).is(expected);
    }

    public static boolean hasBlockTagAtOffset(BlueprintContext context, int dx, int dy, int dz, TagKey<Block> tag) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int x = loc.getBlockX() + dx;
        int z = loc.getBlockZ() + dz;

        if (!isChunkLoaded(level, x, z)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, loc.getBlockY() + dy, z);
        return level.getBlockState(pos).is(tag);
    }

    public static boolean isBlockAtFace(BlueprintContext context, BlockFace face, BlockType type) {
        Location loc = context.getCoreLocation();
        int x = loc.getBlockX() + face.getModX();
        int y = loc.getBlockY() + face.getModY();
        int z = loc.getBlockZ() + face.getModZ();

        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, x, z)) return false;

        Block expected = toNMS(type);
        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, y, z);
        return level.getBlockState(pos).is(expected);
    }

    public static boolean hasBlockTagAtFace(BlueprintContext context, BlockFace face, TagKey<Block> tag) {
        Location loc = context.getCoreLocation();
        int x = loc.getBlockX() + face.getModX();
        int y = loc.getBlockY() + face.getModY();
        int z = loc.getBlockZ() + face.getModZ();

        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, x, z)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, y, z);
        return level.getBlockState(pos).is(tag);
    }

    public static boolean isWaterloggedAtOffset(BlueprintContext context, int dx, int dy, int dz) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int x = loc.getBlockX() + dx;
        int z = loc.getBlockZ() + dz;

        if (!isChunkLoaded(level, x, z)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, loc.getBlockY() + dy, z);
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    public static boolean isLitAtOffset(BlueprintContext context, int dx, int dy, int dz) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int x = loc.getBlockX() + dx;
        int z = loc.getBlockZ() + dz;

        if (!isChunkLoaded(level, x, z)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, loc.getBlockY() + dy, z);
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    public static boolean isFluidAtOffset(BlueprintContext context, int dx, int dy, int dz, TagKey<Fluid> fluidTag) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int x = loc.getBlockX() + dx;
        int z = loc.getBlockZ() + dz;

        if (!isChunkLoaded(level, x, z)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(x, loc.getBlockY() + dy, z);
        return level.getFluidState(pos).is(fluidTag);
    }

    public static int getCoreY(BlueprintContext context) {
        return context.getCoreLocation().getBlockY();
    }

    public static boolean isBelowY(BlueprintContext context, int y) {
        return context.getCoreLocation().getBlockY() < y;
    }

    public static boolean isAboveY(BlueprintContext context, int y) {
        return context.getCoreLocation().getBlockY() > y;
    }

    public static boolean isBiome(BlueprintContext context, ResourceKey<Biome> biomeKey) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.getBiome(pos).is(biomeKey);
    }

    public static boolean isExposedToSky(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.canSeeSky(pos);
    }

    public static boolean isRainingAtCore(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!level.isRaining() || !isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.getBiome(pos).value().hasPrecipitation();
    }

    public static boolean isThunderingAtCore(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!level.isThundering() || !isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.getBiome(pos).value().hasPrecipitation();
    }

    public static boolean isSlimeChunk(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        long seed = level.getSeed();
        long slimeSeed = seed +
            ((long) chunkX * chunkX * 4987142) +
            (chunkX * 5947611L) +
            (long) chunkZ * chunkZ * 4392871L +
            (chunkZ * 389711L) ^ 987234911L;

        Random rnd = new Random(slimeSeed);
        return rnd.nextInt(10) == 0;
    }

    public static int getBlockLight(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return 0;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.getBrightness(LightLayer.BLOCK, pos);
    }

    public static int getSkyLight(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return 0;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.getBrightness(LightLayer.SKY, pos);
    }

    public static boolean isDay(BlueprintContext context) {
        return getServerLevel(context).isBrightOutside();
    }

    public static boolean isNight(BlueprintContext context) {
        return getServerLevel(context).isDarkOutside();
    }

    public static boolean isPlayerSneaking(BlueprintContext context) {
        return context.getPlayer() != null && ((CraftPlayer) context.getPlayer()).getHandle().isCrouching();
    }

    public static boolean isDimension(BlueprintContext context, ResourceKey<Level> dimensionKey) {
        return getServerLevel(context).dimension() == dimensionKey;
    }

    public static boolean hasNeighborSignal(BlueprintContext context) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        if (!isChunkLoaded(level, loc.getBlockX(), loc.getBlockZ())) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return level.hasNeighborSignal(pos);
    }

    public static boolean hasAirClearance(BlueprintContext context, int height) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        int startX = loc.getBlockX();
        int startY = loc.getBlockY() + 1;
        int startZ = loc.getBlockZ();

        if (!isChunkLoaded(level, startX, startZ)) return false;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
        for (int i = 0; i < height; i++) {
            if (!level.getBlockState(pos.set(startX, startY + i, startZ)).isAir()) {
                return false;
            }
        }
        return true;
    }

    public static int countNearbyBlocksCube(BlueprintContext context, BlockType type, int radius) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        Block targetBlock = toNMS(type);

        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        int count = 0;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
        long[] offsets = getCubeOffsets(radius);

        for (long packed : offsets) {
            int dx = (short) (packed & 0xFFFFL);
            int dy = (short) ((packed >>> 16) & 0xFFFFL);
            int dz = (short) ((packed >>> 32) & 0xFFFFL);

            int targetX = cx + dx;
            int targetZ = cz + dz;

            if (isChunkLoaded(level, targetX, targetZ)) {
                if (level.getBlockState(pos.set(targetX, cy + dy, targetZ)).is(targetBlock)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int countNearbyBlocksSphere(BlueprintContext context, BlockType type, int radius) {
        Location loc = context.getCoreLocation();
        ServerLevel level = getServerLevel(context);
        Block targetBlock = toNMS(type);

        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        int count = 0;

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
        long[] offsets = getSphereOffsets(radius);

        for (long packed : offsets) {
            int dx = (short) (packed & 0xFFFFL);
            int dy = (short) ((packed >>> 16) & 0xFFFFL);
            int dz = (short) ((packed >>> 32) & 0xFFFFL);

            int targetX = cx + dx;
            int targetZ = cz + dz;

            if (isChunkLoaded(level, targetX, targetZ)) {
                if (level.getBlockState(pos.set(targetX, cy + dy, targetZ)).is(targetBlock)) {
                    count++;
                }
            }
        }
        return count;
    }
}