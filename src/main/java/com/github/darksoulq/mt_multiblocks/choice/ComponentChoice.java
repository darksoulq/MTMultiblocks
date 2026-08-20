package com.github.darksoulq.mt_multiblocks.choice;

import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serializable;

public interface ComponentChoice extends Serializable {
    boolean matches(BlockData blockData, BlockFace facing);
    BlockData getDisplayData(BlockFace facing);
    BlockState getNMSDisplayData(BlockFace facing);
}