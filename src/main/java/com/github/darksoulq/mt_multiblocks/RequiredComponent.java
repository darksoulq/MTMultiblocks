package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serial;

public final class RequiredComponent implements StructureComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String rawDataString;
    private final Port port;
    private transient BlockData rawDataCache;
    private transient BlockData[] rotatedCache;

    public RequiredComponent(BlockData blockData, Port port) {
        this.rawDataString = blockData.getAsString();
        this.port = port;
        initCache();
    }

    private void initCache() {
        this.rawDataCache = Bukkit.createBlockData(this.rawDataString);
        this.rotatedCache = new BlockData[4];
        this.rotatedCache[0] = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.NORTH);
        this.rotatedCache[1] = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.EAST);
        this.rotatedCache[2] = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.SOUTH);
        this.rotatedCache[3] = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.WEST);
    }

    @Override
    public Port getPort() {
        return port;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public boolean matches(BlockData blockData, BlockFace facing) {
        return blockData.matches(getDisplayData(facing));
    }

    @Override
    public BlockData getDisplayData(BlockFace facing) {
        if (rotatedCache == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedCache[index] : rawDataCache;
    }
}