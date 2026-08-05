package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serial;
import java.io.Serializable;

public class StructureComponent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Port port;
    private final String rawDataString;
    private final boolean isRequired;
    private transient BlockData rawData;
    private transient BlockData[] rotatedCache;

    public StructureComponent(BlockData blockData, Port port, boolean isRequired) {
        this.rawDataString = blockData.getAsString();
        this.port = port;
        this.isRequired = isRequired;
        initCache();
    }

    private void initCache() {
        this.rawData = Bukkit.createBlockData(this.rawDataString);
        this.rotatedCache = new BlockData[4];
        this.rotatedCache[0] = RotationUtil.rotateBlockData(this.rawData, BlockFace.NORTH);
        this.rotatedCache[1] = RotationUtil.rotateBlockData(this.rawData, BlockFace.EAST);
        this.rotatedCache[2] = RotationUtil.rotateBlockData(this.rawData, BlockFace.SOUTH);
        this.rotatedCache[3] = RotationUtil.rotateBlockData(this.rawData, BlockFace.WEST);
    }

    public BlockData getBlockData() {
        if (rawData == null) initCache();
        return rawData;
    }

    public Port getPort() {
        return port;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public BlockData getRotatedData(BlockFace facing) {
        if (rotatedCache == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedCache[index] : rawData;
    }
}