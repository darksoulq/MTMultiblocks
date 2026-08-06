package com.github.darksoulq.mt_multiblocks.choice;

import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serial;

public class TagChoice implements ComponentChoice {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String tagKey;
    private final String defaultDataString;
    private transient Tag<Material> cachedTag;
    private transient BlockData defaultDataCache;

    public TagChoice(Tag<Material> tag, BlockData defaultData) {
        this.tagKey = tag.getKey().toString();
        this.defaultDataString = defaultData.getAsString();
        this.cachedTag = tag;
        this.defaultDataCache = defaultData;
    }

    private void initCache() {
        if (this.defaultDataCache == null && this.defaultDataString != null) {
            this.defaultDataCache = Bukkit.createBlockData(this.defaultDataString);
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
        if (defaultDataCache == null) initCache();
        return RotationUtil.rotateBlockData(defaultDataCache, facing);
    }
}