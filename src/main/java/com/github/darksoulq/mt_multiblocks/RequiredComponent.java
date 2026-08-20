package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.io.Serial;

public final class RequiredComponent implements StructureComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String rawDataString;
    private final Port port;
    private transient BlockData rawDataCache;
    private transient BlockData[] rotatedCache;
    private transient BlockState[] rotatedNMSCache;

    public RequiredComponent(BlockData blockData, Port port) {
        this.rawDataString = blockData.getAsString();
        this.port = port;
        initCache();
    }

    private void initCache() {
        this.rawDataCache = Bukkit.createBlockData(this.rawDataString);
        this.rotatedCache = new BlockData[4];
        this.rotatedNMSCache = new BlockState[4];

        BlockData north = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.NORTH);
        BlockData east = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.EAST);
        BlockData south = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.SOUTH);
        BlockData west = RotationUtil.rotateBlockData(this.rawDataCache, BlockFace.WEST);

        this.rotatedCache[0] = north;
        this.rotatedCache[1] = east;
        this.rotatedCache[2] = south;
        this.rotatedCache[3] = west;

        this.rotatedNMSCache[0] = ((CraftBlockData) north).getState();
        this.rotatedNMSCache[1] = ((CraftBlockData) east).getState();
        this.rotatedNMSCache[2] = ((CraftBlockData) south).getState();
        this.rotatedNMSCache[3] = ((CraftBlockData) west).getState();
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

    @Override
    public BlockState getNMSDisplayData(BlockFace facing) {
        if (rotatedNMSCache == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedNMSCache[index] : rotatedNMSCache[0];
    }
}