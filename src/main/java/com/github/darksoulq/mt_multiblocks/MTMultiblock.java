package com.github.darksoulq.mt_multiblocks;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface MTMultiblock {
    MultiblockBlueprint getBlueprint();
    BlockFace getFacing();
    Location getLocationREADONLY();
}