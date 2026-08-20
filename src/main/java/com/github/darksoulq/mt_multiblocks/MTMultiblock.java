package com.github.darksoulq.mt_multiblocks;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface MTMultiblock {
    MultiblockBlueprint getBlueprint(BlueprintContext context);
    BlockFace getFacing();
    Location getLocationREADONLY();
}