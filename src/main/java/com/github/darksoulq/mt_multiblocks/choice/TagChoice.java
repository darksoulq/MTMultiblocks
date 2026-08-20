package com.github.darksoulq.mt_multiblocks.choice;

import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.io.Serial;

public class TagChoice implements ComponentChoice {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String tagKey;
    private final String defaultDataString;

    private transient Tag<Material> cachedTag;
    private transient BlockData defaultDataCache;

    private transient BlockData[] rotatedCache;
    private transient BlockState[] rotatedNMSCache;

    public TagChoice(Tag<Material> tag, BlockData defaultData) {
        this.tagKey = tag.getKey().toString();
        this.defaultDataString = defaultData.getAsString();
        this.cachedTag = tag;
        this.defaultDataCache = defaultData;
        initCache();
    }

    private void initCache() {
        if (this.defaultDataCache == null && this.defaultDataString != null) {
            this.defaultDataCache = Bukkit.createBlockData(this.defaultDataString);
        }

        if (this.defaultDataCache != null && this.rotatedCache == null) {
            this.rotatedCache = new BlockData[4];
            this.rotatedNMSCache = new BlockState[4];

            BlockData north = RotationUtil.rotateBlockData(this.defaultDataCache, BlockFace.NORTH);
            BlockData east = RotationUtil.rotateBlockData(this.defaultDataCache, BlockFace.EAST);
            BlockData south = RotationUtil.rotateBlockData(this.defaultDataCache, BlockFace.SOUTH);
            BlockData west = RotationUtil.rotateBlockData(this.defaultDataCache, BlockFace.WEST);

            this.rotatedCache[0] = north;
            this.rotatedCache[1] = east;
            this.rotatedCache[2] = south;
            this.rotatedCache[3] = west;

            this.rotatedNMSCache[0] = ((CraftBlockData) north).getState();
            this.rotatedNMSCache[1] = ((CraftBlockData) east).getState();
            this.rotatedNMSCache[2] = ((CraftBlockData) south).getState();
            this.rotatedNMSCache[3] = ((CraftBlockData) west).getState();
        }

        if (this.cachedTag == null && this.tagKey != null) {
            String[] split = tagKey.split(":");
            NamespacedKey key = (split.length == 2) ? new NamespacedKey(split[0], split[1]) : NamespacedKey.minecraft(tagKey);
            this.cachedTag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        }
    }

    @Override
    public boolean matches(BlockData blockData, BlockFace facing) {
        if (cachedTag == null) initCache();
        if (cachedTag == null) return false;
        return cachedTag.isTagged(blockData.getMaterial());
    }

    @Override
    public BlockData getDisplayData(BlockFace facing) {
        if (rotatedCache == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedCache[index] : defaultDataCache;
    }

    @Override
    public BlockState getNMSDisplayData(BlockFace facing) {
        if (rotatedNMSCache == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedNMSCache[index] : rotatedNMSCache[0];
    }
}