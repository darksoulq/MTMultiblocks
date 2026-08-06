package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.port.Port;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serializable;

public sealed interface StructureComponent extends Serializable permits RequiredComponent, OptionalComponent {
    Port getPort();
    boolean isRequired();
    boolean matches(BlockData blockData, BlockFace facing);
    BlockData getDisplayData(BlockFace facing);
}