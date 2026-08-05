package com.github.darksoulq.mt_multiblocks;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public interface PlacementCondition {
    boolean canPlace(Location coreLoc, BlockFace facing, Player player);
}