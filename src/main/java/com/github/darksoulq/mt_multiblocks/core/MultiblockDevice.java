package com.github.darksoulq.mt_multiblocks.core;

import com.github.darksoulq.mt_multiblocks.MTMultiblock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

public abstract class MultiblockDevice extends AddonDevice implements MTMultiblock {

    private BlockFace facing;

    public MultiblockDevice(Location loc) {
        super(loc);
    }

    public void setFacing(BlockFace facing) {
        this.facing = facing;
    }

    @Override
    public BlockFace getFacing() {
        if (this.facing != null) {
            return this.facing;
        }
        if (getLocationREADONLY() != null) {
            Block block = getLocationREADONLY().getBlock();
            BlockData data = block.getBlockData();
            if (data instanceof Directional dir) {
                return dir.getFacing();
            }
            if (data instanceof Rotatable rot) {
                return rot.getRotation();
            }
        }
        return BlockFace.NORTH;
    }
}